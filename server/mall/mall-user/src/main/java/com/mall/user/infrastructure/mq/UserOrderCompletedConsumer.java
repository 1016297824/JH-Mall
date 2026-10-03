package com.mall.user.infrastructure.mq;

import java.nio.charset.StandardCharsets;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.constant.MqTopicConstants;
import com.mall.common.enums.user.BizTypeEnum;
import com.mall.common.mq.MqDedupGuard;
import com.mall.user.service.IMemberService;
import com.mall.user.service.IPointsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 订单完成事件消费者
 *
 * <p>消费订单完成 MQ 消息，为用户增加积分和成长值。
 * 消息体 JSON 示例：{"userId":1,"orderNo":"ORD001","orderAmount":10000,"points":100}</p>
 *
 * <p><b>积分与成长值是不可逆的资产变动</b>，故三条约束必须同时成立：</p>
 * <ol>
 *   <li><b>去重</b>：按 broker 分配的 {@code msgId} + 消费组做 Redis 占位，重投不二次发放</li>
 *   <li><b>失败必须上抛并释放去重标记</b>：原先 {@code catch(Exception)} 只打日志，
 *       等于把失败吞成成功——用户永远拿不到积分且没有任何重试</li>
 *   <li><b>数据缺陷丢弃、异常重投</b>：缺 {@code userId} 的消息重投也治不好，
 *       留痕后丢弃，避免毒丸长期占据补偿队首</li>
 * </ol>
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Component
@RocketMQMessageListener(topic = MqTopicConstants.Order.COMPLETED, consumerGroup = "${rocketmq.consumer.group:mall-user-consumer}")
@Slf4j
@RequiredArgsConstructor
public class UserOrderCompletedConsumer implements RocketMQListener<MessageExt> {

    /** JSON 解析器（无状态、线程安全，按项目约定静态复用） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 去重用的消费组标识，与 {@code @RocketMQMessageListener} 的组名区分：这里要的是稳定常量 */
    private static final String CONSUMER_GROUP = "mall-user-order-completed";

    /** 积分服务 */
    private final IPointsService pointsService;

    /** 会员服务 */
    private final IMemberService memberService;

    /** MQ 消费幂等去重守卫 */
    private final MqDedupGuard dedupGuard;

    /**
     * 消费订单完成事件
     *
     * <p>解析消息中的 userId、orderNo、points、orderAmount 字段：
     * <ul>
     *   <li>points 大于 0 时调用积分服务增加积分</li>
     *   <li>orderAmount 大于 0 时按 100:1 折算成长值并增加</li>
     * </ul>
     * </p>
     *
     * @param message MQ 消息（用 MessageExt 而非 String，以便取到 broker 分配的 msgId 做去重）
     */
    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String msgId = message.getMsgId();

        JsonNode json;
        try {
            json = OBJECT_MAPPER.readTree(body);
        } catch (Exception e) {
            // 解析失败说明投递内容损坏，交由 MQ 重投；此时尚未占位，无需释放
            log.error("订单完成消息解析失败: msgId={}, body={}", msgId, body, e);
            throw new IllegalStateException("订单完成消息解析失败", e);
        }

        Long userId = json.hasNonNull("userId") ? json.get("userId").asLong() : null;
        String orderNo = json.hasNonNull("orderNo") ? json.get("orderNo").asText() : null;

        if (userId == null) {
            // 数据缺陷：重投也治不好，留痕后丢弃
            log.error("订单完成消息缺少 userId，丢弃: msgId={}, body={}", msgId, body);
            return;
        }

        if (!dedupGuard.tryDedup(msgId, CONSUMER_GROUP)) {
            return;
        }

        try {
            grantPoints(userId, orderNo, json);
            grantGrowth(userId, orderNo, json);
        } catch (RuntimeException e) {
            log.error("订单完成事件处理失败，交由 MQ 重试: msgId={}, userId={}, orderNo={}",
                    msgId, userId, orderNo, e);
            try {
                dedupGuard.release(msgId, CONSUMER_GROUP);
            } catch (RuntimeException releaseError) {
                log.error("【需人工介入】释放去重标记失败，重投将被去重拦截: msgId={}", msgId, releaseError);
            }
            throw e;
        }
    }

    /**
     * 发放积分
     *
     * @param userId  用户 ID
     * @param orderNo 订单号（作为业务幂等键）
     * @param json    消息体
     */
    private void grantPoints(Long userId, String orderNo, JsonNode json) {
        JsonNode pointsNode = json.get("points");
        if (pointsNode == null || pointsNode.isNull()) {
            return;
        }
        int points = pointsNode.asInt();
        if (points > 0) {
            pointsService.addPoints(userId, points, BizTypeEnum.ORDER, orderNo);
        }
    }

    /**
     * 发放成长值（按订单金额 100:1 折算）
     *
     * @param userId  用户 ID
     * @param orderNo 订单号（作为业务幂等键）
     * @param json    消息体
     */
    private void grantGrowth(Long userId, String orderNo, JsonNode json) {
        JsonNode orderAmountNode = json.get("orderAmount");
        if (orderAmountNode == null || orderAmountNode.isNull()) {
            return;
        }
        long orderAmount = orderAmountNode.asLong();
        if (orderAmount <= 0) {
            return;
        }
        int growth = (int) (orderAmount / 100);
        if (growth > 0) {
            memberService.addGrowth(userId, growth, BizTypeEnum.ORDER, orderNo);
        }
    }
}

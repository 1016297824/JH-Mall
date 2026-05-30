package com.mall.user.infrastructure.mq;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.constant.MqTopicConstants;
import com.mall.common.enums.user.BizTypeEnum;
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
 * @author JH-Mall
 * @date 2026/05/28
 */
@Component
@RocketMQMessageListener(topic = MqTopicConstants.Order.COMPLETED, consumerGroup = "${rocketmq.consumer.group:mall-user-consumer}")
@Slf4j
@RequiredArgsConstructor
public class UserOrderCompletedConsumer implements RocketMQListener<String> {

    /** JSON 解析器 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 积分服务 */
    private final IPointsService pointsService;

    /** 会员服务 */
    private final IMemberService memberService;

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
     * @param message JSON 格式的消息字符串
     */
    @Override
    public void onMessage(String message) {
        try {
            // 解析订单完成事件的 JSON 消息体
            JsonNode json = objectMapper.readTree(message);
            Long userId = json.has("userId") ? json.get("userId").asLong() : null;
            String orderNo = json.has("orderNo") ? json.get("orderNo").asText() : null;
            JsonNode orderAmountNode = json.get("orderAmount");
            JsonNode pointsNode = json.get("points");

            if (userId == null) {
                log.warn("订单完成事件缺少 userId, message={}", message);
                return;
            }

            // 消费积分：points 大于 0 才增加
            if (pointsNode != null && !pointsNode.isNull()) {
                int points = pointsNode.asInt();
                if (points > 0) {
                    pointsService.addPoints(userId, points, BizTypeEnum.ORDER, orderNo);
                }
            }
            // 消费成长值：按订单金额 100:1 折算
            if (orderAmountNode != null && !orderAmountNode.isNull()) {
                long orderAmount = orderAmountNode.asLong();
                if (orderAmount > 0) {
                    int growth = (int) (orderAmount / 100);
                    if (growth > 0) {
                        memberService.addGrowth(userId, growth, BizTypeEnum.ORDER, orderNo);
                    }
                }
            }
        } catch (Exception e) {
            log.error("消费订单完成事件失败: message={}", message, e);
        }
    }
}

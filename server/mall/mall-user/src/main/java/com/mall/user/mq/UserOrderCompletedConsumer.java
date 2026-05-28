package com.mall.user.mq;

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
 * <p>消费订单完成 MQ 消息，为用户增加积分和成长值</p>
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Component
@RocketMQMessageListener(topic = MqTopicConstants.Order.COMPLETED, consumerGroup = "${rocketmq.consumer.group:mall-user-consumer}")
@Slf4j
@RequiredArgsConstructor
public class UserOrderCompletedConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final IPointsService pointsService;

    private final IMemberService memberService;

    /**
     * 消费订单完成事件
     *
     * <p>消息体: {"userId":1,"orderNo":"ORD001","orderAmount":10000,"points":100}</p>
     *
     * @param message JSON 消息字符串
     */
    @Override
    public void onMessage(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            Long userId = json.has("userId") ? json.get("userId").asLong() : null;
            String orderNo = json.has("orderNo") ? json.get("orderNo").asText() : null;
            JsonNode orderAmountNode = json.get("orderAmount");
            JsonNode pointsNode = json.get("points");

            if (pointsNode != null && !pointsNode.isNull()) {
                int points = pointsNode.asInt();
                if (points > 0) {
                    pointsService.addPoints(userId, points, BizTypeEnum.ORDER, orderNo);
                }
            }
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

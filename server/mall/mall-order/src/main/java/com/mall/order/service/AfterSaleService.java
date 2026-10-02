package com.mall.order.service;

import com.mall.order.VO.AfterSaleVO;
import com.mall.order.dto.request.SubmitAfterSaleRequest;

import java.util.List;

/**
 * 售后服务
 *
 * <p>对应设计文档 §8。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
public interface AfterSaleService {

    /**
     * 提交售后申请
     *
     * <p>校验订单状态可退→ 创建售后单（PENDING）→ 仅退款且金额未超阈值时自动审核通过。</p>
     *
     * @param userId 用户 ID
     * @param req    申请请求
     * @return 售后单号
     */
    String submit(Long userId, SubmitAfterSaleRequest req);

    /**
     * 查询我的售后列表
     *
     * @param userId 用户 ID
     * @param orderNo 订单号，传 null 查全部
     * @return 售后单列表
     */
    List<AfterSaleVO> list(Long userId, String orderNo);

    /**
     * 管理端审核通过并发起退款
     *
     * @param afterSaleId 售后单 ID
     * @param remark      审核意见
     */
    void approve(Long afterSaleId, String remark);

    /**
     * 管理端审核驳回
     *
     * @param afterSaleId 售后单 ID
     * @param remark      驳回原因
     */
    void reject(Long afterSaleId, String remark);

    /**
     * 退款成功回调（由 MQ 消费者调用）
     *
     * <p>退货退款场景会回补库存。</p>
     *
     * @param afterSaleNo 售后单号
     * @param refundAmount 实际退款金额（单位：分）
     */
    void refundCallback(String afterSaleNo, Long refundAmount);

    /**
     * 退款失败回调（由 MQ 消费者调用）
     *
     * @param afterSaleNo 售后单号
     */
    void refundFailedCallback(String afterSaleNo);
}
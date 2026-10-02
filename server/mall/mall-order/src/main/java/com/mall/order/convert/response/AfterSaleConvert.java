package com.mall.order.convert.response;

import com.mall.common.enums.order.AfterSaleStatusEnum;
import com.mall.common.enums.order.AfterSaleTypeEnum;
import com.mall.order.DO.MallAfterSaleDO;
import com.mall.order.VO.AfterSaleVO;

import java.util.HashMap;
import java.util.Map;

/**
 * 售后转换器
 *
 * <p>静态方法，无状态。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
public class AfterSaleConvert {

    private static final Map<Integer, AfterSaleTypeEnum> TYPE_MAP = new HashMap<>();
    private static final Map<Integer, AfterSaleStatusEnum> STATUS_MAP = new HashMap<>();

    static {
        for (AfterSaleTypeEnum type : AfterSaleTypeEnum.values()) {
            TYPE_MAP.put(type.getCode(), type);
        }
        for (AfterSaleStatusEnum status : AfterSaleStatusEnum.values()) {
            STATUS_MAP.put(status.getCode(), status);
        }
    }

    private AfterSaleConvert() {
    }

    public static AfterSaleVO toVO(MallAfterSaleDO afterSale, String orderNo) {
        AfterSaleVO vo = new AfterSaleVO();
        vo.setId(afterSale.getId());
        vo.setAfterSaleNo(afterSale.getAfterSaleNo());
        vo.setOrderNo(orderNo);
        vo.setOrderItemId(afterSale.getOrderItemId());
        vo.setReason(afterSale.getReason());
        vo.setAmount(afterSale.getAmount());
        vo.setApplyTime(afterSale.getApplyTime());
        vo.setApproveTime(afterSale.getApproveTime());
        vo.setApproveRemark(afterSale.getApproveRemark());
        vo.setReturnExpressCompany(afterSale.getReturnExpressCompany());
        vo.setReturnExpressNo(afterSale.getReturnExpressNo());

        AfterSaleTypeEnum type = TYPE_MAP.get(afterSale.getAfterSaleType());
        vo.setAfterSaleTypeDesc(type == null ? null : type.getDescription());
        AfterSaleStatusEnum status = STATUS_MAP.get(afterSale.getAfterSaleStatus());
        vo.setAfterSaleStatusDesc(status == null ? null : status.getDescription());
        return vo;
    }
}
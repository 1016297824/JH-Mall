package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.DO.MallAfterSaleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后单 Mapper
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Mapper
public interface MallAfterSaleMapper extends BaseMapper<MallAfterSaleDO> {

    /**
     * 按售后单号查询
     *
     * @param afterSaleNo 售后单号
     * @return 售后单，不存在返回 null
     */
    default MallAfterSaleDO selectByAfterSaleNo(String afterSaleNo) {
        return selectOne(new LambdaQueryWrapper<MallAfterSaleDO>()
                .eq(MallAfterSaleDO::getAfterSaleNo, afterSaleNo)
                .eq(MallAfterSaleDO::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    /**
     * 按订单 ID 查询售后单
     *
     * @param orderId 订单 ID
     * @return 该订单下的售后单列表
     */
    default List<MallAfterSaleDO> selectByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapper<MallAfterSaleDO>()
                .eq(MallAfterSaleDO::getOrderId, orderId)
                .eq(MallAfterSaleDO::getIsDeleted, 0)
                .orderByDesc(MallAfterSaleDO::getApplyTime));
    }

    /**
     * 更新售后状态
     *
     * <p>管理端审核（approve / reject）与退款回调（success / fail）共用。</p>
     *
     * @param id           售后单 ID
     * @param targetStatus 目标状态，取值见 AfterSaleStatusEnum
     * @param approveTime  审核时间，仅审核时传
     * @param approveRemark 审核意见，可为 null
     * @return 影响行数
     */
    @Update("UPDATE mall_order_after_sale SET after_sale_status = #{targetStatus}, " +
            "approve_time = #{approveTime}, approve_remark = #{approveRemark}, " +
            "update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int updateStatus(@Param("id") Long id,
                     @Param("targetStatus") Integer targetStatus,
                     @Param("approveTime") LocalDateTime approveTime,
                     @Param("approveRemark") String approveRemark);

    /**
     * 记录退货物流信息与商家收货时间
     *
     * <p>退货退款场景：收到退货后据此回补库存。</p>
     *
     * @param id                 售后单 ID
     * @param expressCompany     退货物流公司
     * @param expressNo          退货物流单号
     * @param receiptTime        商家确认收货时间
     * @return 影响行数
     */
    @Update("UPDATE mall_order_after_sale SET return_express_company = #{expressCompany}, " +
            "return_express_no = #{expressNo}, receipt_time = #{receiptTime}, update_time = NOW() " +
            "WHERE id = #{id} AND is_deleted = 0")
    int updateReturnInfo(@Param("id") Long id,
                         @Param("expressCompany") String expressCompany,
                         @Param("expressNo") String expressNo,
                         @Param("receiptTime") LocalDateTime receiptTime);
}
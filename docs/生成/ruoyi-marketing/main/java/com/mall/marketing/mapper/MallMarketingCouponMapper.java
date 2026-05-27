package com.mall.marketing.mapper;

import java.util.List;
import com.mall.marketing.DO.MallMarketingCoupon;
import com.mall.marketing.DO.MallMarketingCouponRecord;

/**
 * 优惠券定义Mapper接口
 *
 * @author ruoyi
 * @date 2026-05-21
 */
public interface MallMarketingCouponMapper
{
    /**
     * 查询优惠券定义
     *
     * @param id 优惠券定义主键
     * @return 优惠券定义
     */
    public MallMarketingCoupon selectMallMarketingCouponById(String id);

    /**
     * 查询优惠券定义列表
     *
     * @param mallMarketingCoupon 优惠券定义
     * @return 优惠券定义集合
     */
    public List<MallMarketingCoupon> selectMallMarketingCouponList(MallMarketingCoupon mallMarketingCoupon);

    /**
     * 新增优惠券定义
     *
     * @param mallMarketingCoupon 优惠券定义
     * @return 结果
     */
    public int insertMallMarketingCoupon(MallMarketingCoupon mallMarketingCoupon);

    /**
     * 修改优惠券定义
     *
     * @param mallMarketingCoupon 优惠券定义
     * @return 结果
     */
    public int updateMallMarketingCoupon(MallMarketingCoupon mallMarketingCoupon);

    /**
     * 删除优惠券定义
     *
     * @param id 优惠券定义主键
     * @return 结果
     */
    public int deleteMallMarketingCouponById(String id);

    /**
     * 批量删除优惠券定义
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMallMarketingCouponByIds(String[] ids);

    /**
     * 批量删除用户优惠券记录
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMallMarketingCouponRecordByCouponIds(String[] ids);

    /**
     * 批量新增用户优惠券记录
     *
     * @param mallMarketingCouponRecordList 用户优惠券记录列表
     * @return 结果
     */
    public int batchMallMarketingCouponRecord(List<MallMarketingCouponRecord> mallMarketingCouponRecordList);


    /**
     * 通过优惠券定义主键删除用户优惠券记录信息
     *
     * @param id 优惠券定义ID
     * @return 结果
     */
    public int deleteMallMarketingCouponRecordByCouponId(String id);
}

package com.mall.admin.marketing.service;

import java.util.List;
import com.mall.admin.marketing.domain.MallMarketingCoupon;

/**
 * 优惠券定义Service接口
 *
 * @author ruoyi
 * @date 2026-05-21
 */
public interface IMallMarketingCouponService
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
     * 批量删除优惠券定义
     *
     * @param ids 需要删除的优惠券定义主键集合
     * @return 结果
     */
    public int deleteMallMarketingCouponByIds(String[] ids);

    /**
     * 删除优惠券定义信息
     *
     * @param id 优惠券定义主键
     * @return 结果
     */
    public int deleteMallMarketingCouponById(String id);
}

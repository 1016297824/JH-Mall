package com.mall.marketing.service;

import com.mall.marketing.dto.request.CreateCouponReq;
import com.mall.marketing.dto.request.UpdateCouponReq;
import com.mall.marketing.dto.response.CouponDefResp;

import java.util.List;

/**
 * 优惠券定义服务
 *
 * <p>券模板的查询与维护。设计依据：
 * {@code docs/design/14_mall-marketing详细设计.md} §3.1。</p>
 *
 * <p>{@code createCouponDef} / {@code updateCouponDef} / {@code deleteCouponDef} 的管理端
 * 接口由若依代码生成器产出，但其中的状态相关规则（草稿可全改、已发布只可改名、
 * 未发布才可物理删除）是生成器不会产生的业务逻辑，故集中在本服务。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
public interface CouponDefService {

    /**
     * 查询可领取的优惠券列表（C 端，无需登录）
     *
     * <p>条件：已发布 + 未过有效期 + 仍有剩余量。</p>
     *
     * @param limit 单次返回上限
     * @return 可领券列表
     */
    List<CouponDefResp> listAvailableCoupons(int limit);

    /**
     * 创建优惠券定义
     *
     * <p>新建的券一律为「草稿」态，需另行发布。</p>
     *
     * @param req 创建请求
     * @return 新建券定义 ID
     */
    Long createCouponDef(CreateCouponReq req);

    /**
     * 修改优惠券定义
     *
     * <p>草稿态可改全部字段；已发布态<b>仅允许改名称</b>；已结束/已废弃态抛 A0503。</p>
     *
     * @param req 修改请求（含券定义 ID）
     */
    void updateCouponDef(UpdateCouponReq req);

    /**
     * 删除优惠券定义
     *
     * <p>草稿态物理删除；其余状态置为「已废弃」（{@code coupon_status=3}），
     * 以保证已领出的券记录仍可追溯到券定义。</p>
     *
     * @param id 券定义 ID
     */
    void deleteCouponDef(Long id);
}

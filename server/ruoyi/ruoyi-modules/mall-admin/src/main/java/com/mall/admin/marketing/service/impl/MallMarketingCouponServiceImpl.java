package com.mall.admin.marketing.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import com.ruoyi.common.core.utils.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import com.mall.admin.marketing.domain.MallMarketingCouponRecord;
import com.mall.admin.marketing.mapper.MallMarketingCouponMapper;
import com.mall.admin.marketing.domain.MallMarketingCoupon;
import com.mall.admin.marketing.service.IMallMarketingCouponService;

/**
 * 优惠券定义Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-21
 */
@Service
public class MallMarketingCouponServiceImpl implements IMallMarketingCouponService
{
    @Autowired
    private MallMarketingCouponMapper mallMarketingCouponMapper;

    /**
     * 查询优惠券定义
     *
     * @param id 优惠券定义主键
     * @return 优惠券定义
     */
    @Override
    public MallMarketingCoupon selectMallMarketingCouponById(String id)
    {
        return mallMarketingCouponMapper.selectMallMarketingCouponById(id);
    }

    /**
     * 查询优惠券定义列表
     *
     * @param mallMarketingCoupon 优惠券定义
     * @return 优惠券定义
     */
    @Override
    public List<MallMarketingCoupon> selectMallMarketingCouponList(MallMarketingCoupon mallMarketingCoupon)
    {
        return mallMarketingCouponMapper.selectMallMarketingCouponList(mallMarketingCoupon);
    }

    /**
     * 新增优惠券定义
     *
     * @param mallMarketingCoupon 优惠券定义
     * @return 结果
     */
    @Transactional
    @Override
    public int insertMallMarketingCoupon(MallMarketingCoupon mallMarketingCoupon)
    {
        mallMarketingCoupon.setCreateTime(DateUtils.getNowDate());
        int rows = mallMarketingCouponMapper.insertMallMarketingCoupon(mallMarketingCoupon);
        insertMallMarketingCouponRecord(mallMarketingCoupon);
        return rows;
    }

    /**
     * 修改优惠券定义
     *
     * @param mallMarketingCoupon 优惠券定义
     * @return 结果
     */
    @Transactional
    @Override
    public int updateMallMarketingCoupon(MallMarketingCoupon mallMarketingCoupon)
    {
        mallMarketingCoupon.setUpdateTime(DateUtils.getNowDate());
        mallMarketingCouponMapper.deleteMallMarketingCouponRecordByCouponId(mallMarketingCoupon.getId());
        insertMallMarketingCouponRecord(mallMarketingCoupon);
        return mallMarketingCouponMapper.updateMallMarketingCoupon(mallMarketingCoupon);
    }

    /**
     * 批量删除优惠券定义
     *
     * @param ids 需要删除的优惠券定义主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteMallMarketingCouponByIds(String[] ids)
    {
        mallMarketingCouponMapper.deleteMallMarketingCouponRecordByCouponIds(ids);
        return mallMarketingCouponMapper.deleteMallMarketingCouponByIds(ids);
    }

    /**
     * 删除优惠券定义信息
     *
     * @param id 优惠券定义主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteMallMarketingCouponById(String id)
    {
        mallMarketingCouponMapper.deleteMallMarketingCouponRecordByCouponId(id);
        return mallMarketingCouponMapper.deleteMallMarketingCouponById(id);
    }

    /**
     * 新增用户优惠券记录信息
     *
     * @param mallMarketingCoupon 优惠券定义对象
     */
    public void insertMallMarketingCouponRecord(MallMarketingCoupon mallMarketingCoupon)
    {
        List<MallMarketingCouponRecord> mallMarketingCouponRecordList = mallMarketingCoupon.getMallMarketingCouponRecordList();
        String id = mallMarketingCoupon.getId();
        if (StringUtils.isNotNull(mallMarketingCouponRecordList))
        {
            List<MallMarketingCouponRecord> list = new ArrayList<MallMarketingCouponRecord>();
            for (MallMarketingCouponRecord mallMarketingCouponRecord : mallMarketingCouponRecordList)
            {
                mallMarketingCouponRecord.setCouponId(id);
                list.add(mallMarketingCouponRecord);
            }
            if (list.size() > 0)
            {
                mallMarketingCouponMapper.batchMallMarketingCouponRecord(list);
            }
        }
    }
}

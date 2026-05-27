package com.mall.user.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.user.mapper.MallUserAddressMapper;
import com.mall.user.domain.MallUserAddress;
import com.mall.user.service.IMallUserAddressService;

/**
 * 地址簿Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-18
 */
@Service
public class MallUserAddressServiceImpl implements IMallUserAddressService 
{
    @Autowired
    private MallUserAddressMapper mallUserAddressMapper;

    /**
     * 查询地址簿
     * 
     * @param id 地址簿主键
     * @return 地址簿
     */
    @Override
    public MallUserAddress selectMallUserAddressById(String id)
    {
        return mallUserAddressMapper.selectMallUserAddressById(id);
    }

    /**
     * 查询地址簿列表
     * 
     * @param mallUserAddress 地址簿
     * @return 地址簿
     */
    @Override
    public List<MallUserAddress> selectMallUserAddressList(MallUserAddress mallUserAddress)
    {
        return mallUserAddressMapper.selectMallUserAddressList(mallUserAddress);
    }

    /**
     * 新增地址簿
     * 
     * @param mallUserAddress 地址簿
     * @return 结果
     */
    @Override
    public int insertMallUserAddress(MallUserAddress mallUserAddress)
    {
        mallUserAddress.setCreateTime(DateUtils.getNowDate());
        return mallUserAddressMapper.insertMallUserAddress(mallUserAddress);
    }

    /**
     * 修改地址簿
     * 
     * @param mallUserAddress 地址簿
     * @return 结果
     */
    @Override
    public int updateMallUserAddress(MallUserAddress mallUserAddress)
    {
        mallUserAddress.setUpdateTime(DateUtils.getNowDate());
        return mallUserAddressMapper.updateMallUserAddress(mallUserAddress);
    }

    /**
     * 批量删除地址簿
     * 
     * @param ids 需要删除的地址簿主键
     * @return 结果
     */
    @Override
    public int deleteMallUserAddressByIds(String[] ids)
    {
        return mallUserAddressMapper.deleteMallUserAddressByIds(ids);
    }

    /**
     * 删除地址簿信息
     * 
     * @param id 地址簿主键
     * @return 结果
     */
    @Override
    public int deleteMallUserAddressById(String id)
    {
        return mallUserAddressMapper.deleteMallUserAddressById(id);
    }
}

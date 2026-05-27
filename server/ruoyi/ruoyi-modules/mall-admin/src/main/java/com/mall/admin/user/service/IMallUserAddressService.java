package com.mall.user.service;

import java.util.List;
import com.mall.user.domain.MallUserAddress;

/**
 * 地址簿Service接口
 * 
 * @author ruoyi
 * @date 2026-05-18
 */
public interface IMallUserAddressService 
{
    /**
     * 查询地址簿
     * 
     * @param id 地址簿主键
     * @return 地址簿
     */
    MallUserAddress selectMallUserAddressById(String id);

    /**
     * 查询地址簿列表
     * 
     * @param mallUserAddress 地址簿
     * @return 地址簿集合
     */
    List<MallUserAddress> selectMallUserAddressList(MallUserAddress mallUserAddress);

    /**
     * 新增地址簿
     * 
     * @param mallUserAddress 地址簿
     * @return 结果
     */
    int insertMallUserAddress(MallUserAddress mallUserAddress);

    /**
     * 修改地址簿
     * 
     * @param mallUserAddress 地址簿
     * @return 结果
     */
    int updateMallUserAddress(MallUserAddress mallUserAddress);

    /**
     * 批量删除地址簿
     * 
     * @param ids 需要删除的地址簿主键集合
     * @return 结果
     */
    int deleteMallUserAddressByIds(String[] ids);

    /**
     * 删除地址簿信息
     * 
     * @param id 地址簿主键
     * @return 结果
     */
    int deleteMallUserAddressById(String id);
}

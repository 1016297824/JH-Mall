package com.mall.user.service;

import com.mall.user.VO.AddressVO;

import java.util.List;

/**
 * 地址服务接口
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
public interface IAddressService {

    /**
     * 查询用户地址列表
     *
     * @param userId 用户ID
     * @return 地址列表
     */
    List<AddressVO> listAddresses(Long userId);

    /**
     * 新增地址
     *
     * @param userId  用户ID
     * @param request 地址信息
     * @return 新增后的地址VO
     */
    AddressVO createAddress(Long userId, AddressVO request);

    /**
     * 修改地址
     *
     * @param userId    用户ID
     * @param addressId 地址ID
     * @param request   地址信息
     * @return 修改后的地址VO
     */
    AddressVO updateAddress(Long userId, Long addressId, AddressVO request);

    /**
     * 删除地址
     *
     * @param userId    用户ID
     * @param addressId 地址ID
     */
    void deleteAddress(Long userId, Long addressId);

    /**
     * 设置默认地址
     *
     * @param userId    用户ID
     * @param addressId 地址ID
     */
    void setDefault(Long userId, Long addressId);
}

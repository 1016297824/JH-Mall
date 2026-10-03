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

    /**
     * 校验收货地址归属（供 mall-order 下单前校验，防越权使用他人地址）
     *
     * <p>与增删改不同，本方法<b>不抛异常</b>：Feign 端按 {@code boolean} 消费，
     * 地址不存在 / 已删除 / 不属于该用户一律返回 {@code false}。</p>
     *
     * @param userId    用户 ID
     * @param addressId 地址 ID
     * @return 地址存在、未删除且属于该用户返回 true
     */
    boolean validateAddressOwnership(Long userId, Long addressId);
}

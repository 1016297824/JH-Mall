package com.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.user.DO.MallUserAddressDO;
import com.mall.user.config.MallUserConfigProperties;
import com.mall.user.mapper.MallUserAddressMapper;
import com.mall.user.service.IAddressService;
import com.mall.user.VO.AddressVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 地址服务实现类
 *
 * <p>提供用户收货地址的增删改查及默认地址设置，每个用户最多
 * {mallUserConfigProperties.address.maxCount} 个地址</p>
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements IAddressService {

    private final MallUserAddressMapper mallUserAddressMapper;

    private final MallUserConfigProperties mallUserConfigProperties;

    /**
     * 查询用户收货地址列表
     *
     * <p>默认地址排在最前，其余按创建时间倒序排列</p>
     *
     * @param userId 用户 ID
     * @return 地址 VO 列表
     */
    @Override
    public List<AddressVO> listAddresses(Long userId) {
        LambdaQueryWrapper<MallUserAddressDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MallUserAddressDO::getUserId, userId)
                .eq(MallUserAddressDO::getIsDeleted, 0)
                .orderByDesc(MallUserAddressDO::getIsDefault)
                .orderByDesc(MallUserAddressDO::getCreateTime);
        List<MallUserAddressDO> list = mallUserAddressMapper.selectList(wrapper);
        return list.stream().map(this::toAddressVO).collect(Collectors.toList());
    }

    /**
     * 新增收货地址
     *
     * <p>创建前校验地址数量是否已达上限</p>
     *
     * @param userId  用户 ID
     * @param request 地址信息
     * @return 创建后的地址 VO
     * @throws BusinessException 地址数量超限时抛出 ADDRESS_LIMIT
     */
    @Override
    public AddressVO createAddress(Long userId, AddressVO request) {
        // 校验当前地址数量是否已达上限
        int maxCount = mallUserConfigProperties.getAddress().getMaxCount();
        Long count = mallUserAddressMapper.selectCount(
                new LambdaQueryWrapper<MallUserAddressDO>()
                        .eq(MallUserAddressDO::getUserId, userId)
                        .eq(MallUserAddressDO::getIsDeleted, 0)
        );
        if (count >= maxCount) {
            throw new BusinessException(ErrorCode.ADDRESS_LIMIT);
        }

        MallUserAddressDO addressDO = new MallUserAddressDO();
        addressDO.setUserId(userId);
        fillAddressDO(addressDO, request);
        addressDO.setIsDeleted(0);
        addressDO.setCreateTime(LocalDateTime.now());
        addressDO.setUpdateTime(LocalDateTime.now());
        mallUserAddressMapper.insert(addressDO);

        log.info("新增地址成功, userId={}, addressId={}", userId, addressDO.getId());
        return toAddressVO(addressDO);
    }

    /**
     * 修改收货地址
     *
     * @param userId    用户 ID
     * @param addressId 地址 ID
     * @param request   修改内容
     * @return 更新后的地址 VO
     * @throws BusinessException 地址不存在或不属于当前用户
     */
    @Override
    public AddressVO updateAddress(Long userId, Long addressId, AddressVO request) {
        MallUserAddressDO addressDO = getAddressById(addressId);
        checkOwnership(addressDO, userId);

        fillAddressDO(addressDO, request);
        addressDO.setUpdateTime(LocalDateTime.now());
        mallUserAddressMapper.updateById(addressDO);

        log.info("修改地址成功, userId={}, addressId={}", userId, addressId);
        return toAddressVO(addressDO);
    }

    /**
     * 软删除收货地址
     *
     * @param userId    用户 ID
     * @param addressId 地址 ID
     * @throws BusinessException 地址不存在或不属于当前用户
     */
    @Override
    public void deleteAddress(Long userId, Long addressId) {
        MallUserAddressDO addressDO = getAddressById(addressId);
        checkOwnership(addressDO, userId);

        addressDO.setIsDeleted(1);
        addressDO.setUpdateTime(LocalDateTime.now());
        mallUserAddressMapper.updateById(addressDO);

        log.info("删除地址成功, userId={}, addressId={}", userId, addressId);
    }

    /**
     * 设置默认收货地址
     *
     * <p>先清除该用户所有默认标记，再设置指定地址为默认，保证仅一个默认地址</p>
     *
     * @param userId    用户 ID
     * @param addressId 地址 ID
     * @throws BusinessException 地址不存在或不属于当前用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long addressId) {
        MallUserAddressDO addressDO = getAddressById(addressId);
        checkOwnership(addressDO, userId);

        // 先清除该用户所有地址的默认标记，再设置新的默认地址
        mallUserAddressMapper.clearDefault(userId);
        addressDO.setIsDefault(1);
        addressDO.setUpdateTime(LocalDateTime.now());
        mallUserAddressMapper.updateById(addressDO);

        log.info("设置默认地址成功, userId={}, addressId={}", userId, addressId);
    }

    /**
     * 根据地址 ID 查询地址
     *
     * @param addressId 地址 ID
     * @return 地址 DO
     * @throws BusinessException 地址不存在或已删除
     */
    private MallUserAddressDO getAddressById(Long addressId) {
        MallUserAddressDO addressDO = mallUserAddressMapper.selectById(addressId);
        if (addressDO == null || addressDO.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return addressDO;
    }

    /**
     * 校验地址所有权
     *
     * @param addressDO 地址 DO
     * @param userId    用户 ID
     * @throws BusinessException 地址不属于当前用户
     */
    private void checkOwnership(MallUserAddressDO addressDO, Long userId) {
        if (!addressDO.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }
    }

    /**
     * 将地址 VO 的字段填充到地址 DO
     *
     * <p>仅填充传入的非空字段</p>
     *
     * @param addressDO 目标地址 DO
     * @param request   源地址 VO
     */
    private void fillAddressDO(MallUserAddressDO addressDO, AddressVO request) {
        addressDO.setReceiverName(request.getReceiverName());
        addressDO.setReceiverPhone(request.getReceiverPhone());
        addressDO.setProvince(request.getProvince());
        addressDO.setCity(request.getCity());
        addressDO.setDistrict(request.getDistrict());
        addressDO.setDetailAddress(request.getDetailAddress());
        addressDO.setZipCode(request.getZipCode());
        addressDO.setLabel(request.getLabel());
        if (request.getIsDefault() != null && request.getIsDefault()) {
            addressDO.setIsDefault(1);
        }
    }

    /**
     * 将地址 DO 转换为地址 VO
     *
     * @param addressDO 地址 DO
     * @return 地址 VO
     */
    private AddressVO toAddressVO(MallUserAddressDO addressDO) {
        AddressVO vo = new AddressVO();
        vo.setAddressId(String.valueOf(addressDO.getId()));
        vo.setReceiverName(addressDO.getReceiverName());
        vo.setReceiverPhone(addressDO.getReceiverPhone());
        vo.setProvince(addressDO.getProvince());
        vo.setCity(addressDO.getCity());
        vo.setDistrict(addressDO.getDistrict());
        vo.setDetailAddress(addressDO.getDetailAddress());
        vo.setZipCode(addressDO.getZipCode());
        vo.setIsDefault(addressDO.getIsDefault() != null && addressDO.getIsDefault() == 1);
        vo.setLabel(addressDO.getLabel());
        return vo;
    }
}

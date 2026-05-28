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
 * @author JH-Mall
 * @date 2026/05/28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements IAddressService {

    private final MallUserAddressMapper mallUserAddressMapper;

    private final MallUserConfigProperties mallUserConfigProperties;

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

    @Override
    public AddressVO createAddress(Long userId, AddressVO request) {
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

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        MallUserAddressDO addressDO = getAddressById(addressId);
        checkOwnership(addressDO, userId);

        addressDO.setIsDeleted(1);
        addressDO.setUpdateTime(LocalDateTime.now());
        mallUserAddressMapper.updateById(addressDO);

        log.info("删除地址成功, userId={}, addressId={}", userId, addressId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long addressId) {
        MallUserAddressDO addressDO = getAddressById(addressId);
        checkOwnership(addressDO, userId);

        mallUserAddressMapper.clearDefault(userId);
        addressDO.setIsDefault(1);
        addressDO.setUpdateTime(LocalDateTime.now());
        mallUserAddressMapper.updateById(addressDO);

        log.info("设置默认地址成功, userId={}, addressId={}", userId, addressId);
    }

    private MallUserAddressDO getAddressById(Long addressId) {
        MallUserAddressDO addressDO = mallUserAddressMapper.selectById(addressId);
        if (addressDO == null || addressDO.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return addressDO;
    }

    private void checkOwnership(MallUserAddressDO addressDO, Long userId) {
        if (!addressDO.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }
    }

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

package com.mall.user.service.impl;

import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.user.DO.MallUserAddressDO;
import com.mall.user.config.MallUserConfigProperties;
import com.mall.user.mapper.MallUserAddressMapper;
import com.mall.user.vo.AddressVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AddressServiceImpl 单元测试
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private MallUserAddressMapper mallUserAddressMapper;

    @Mock
    private MallUserConfigProperties mallUserConfigProperties;

    @InjectMocks
    private AddressServiceImpl addressService;

    private MallUserAddressDO buildAddressDO(Long id, Long userId) {
        MallUserAddressDO addressDO = new MallUserAddressDO();
        addressDO.setId(id);
        addressDO.setUserId(userId);
        addressDO.setReceiverName("张三");
        addressDO.setReceiverPhone("13800138000");
        addressDO.setProvince("广东省");
        addressDO.setCity("深圳市");
        addressDO.setDistrict("南山区");
        addressDO.setDetailAddress("科技园路1号");
        addressDO.setIsDefault(id == 1L ? 1 : 0);
        addressDO.setIsDeleted(0);
        addressDO.setCreateTime(LocalDateTime.now());
        addressDO.setUpdateTime(LocalDateTime.now());
        return addressDO;
    }

    private AddressVO buildAddressRequest() {
        AddressVO vo = new AddressVO();
        vo.setReceiverName("张三");
        vo.setReceiverPhone("13800138000");
        vo.setProvince("广东省");
        vo.setCity("深圳市");
        vo.setDistrict("南山区");
        vo.setDetailAddress("科技园路1号");
        vo.setIsDefault(false);
        return vo;
    }

    @Test
    void listAddressesShouldReturnSortedList() {
        MallUserAddressDO addr1 = buildAddressDO(1L, 1L);
        addr1.setIsDefault(1);
        MallUserAddressDO addr2 = buildAddressDO(2L, 1L);
        addr2.setIsDefault(0);

        when(mallUserAddressMapper.selectList(any())).thenReturn(Arrays.asList(addr1, addr2));

        List<AddressVO> result = addressService.listAddresses(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0).getIsDefault());
        assertFalse(result.get(1).getIsDefault());
    }

    @Test
    void addAddressShouldSucceed() {
        MallUserConfigProperties.Address addrConfig = new MallUserConfigProperties.Address();
        when(mallUserConfigProperties.getAddress()).thenReturn(addrConfig);
        when(mallUserAddressMapper.selectCount(any())).thenReturn(0L);
        when(mallUserAddressMapper.insert(any(MallUserAddressDO.class))).thenReturn(1);

        AddressVO request = buildAddressRequest();
        AddressVO result = addressService.addAddress(1L, request);

        assertNotNull(result);
        verify(mallUserAddressMapper).insert(any(MallUserAddressDO.class));
    }

    @Test
    void addAddressShouldThrowWhenExceedLimit() {
        MallUserConfigProperties.Address addrConfig = new MallUserConfigProperties.Address();
        when(mallUserConfigProperties.getAddress()).thenReturn(addrConfig);
        when(mallUserAddressMapper.selectCount(any())).thenReturn(20L);

        AddressVO request = buildAddressRequest();
        assertThrows(BusinessException.class, () -> addressService.addAddress(1L, request));
    }

    @Test
    void updateAddressShouldSucceed() {
        MallUserAddressDO addressDO = buildAddressDO(1L, 1L);
        when(mallUserAddressMapper.selectById(1L)).thenReturn(addressDO);
        when(mallUserAddressMapper.updateById(any(MallUserAddressDO.class))).thenReturn(1);

        AddressVO request = buildAddressRequest();
        request.setReceiverName("李四");
        AddressVO result = addressService.updateAddress(1L, 1L, request);

        assertNotNull(result);
        assertEquals("李四", result.getReceiverName());
        verify(mallUserAddressMapper).updateById(any(MallUserAddressDO.class));
    }

    @Test
    void updateAddressShouldThrowWhenNotOwner() {
        MallUserAddressDO addressDO = buildAddressDO(1L, 2L);
        when(mallUserAddressMapper.selectById(1L)).thenReturn(addressDO);

        AddressVO request = buildAddressRequest();
        assertThrows(BusinessException.class, () -> addressService.updateAddress(1L, 1L, request));
    }

    @Test
    void deleteAddressShouldSucceed() {
        MallUserAddressDO addressDO = buildAddressDO(1L, 1L);
        when(mallUserAddressMapper.selectById(1L)).thenReturn(addressDO);
        when(mallUserAddressMapper.updateById(any(MallUserAddressDO.class))).thenReturn(1);

        addressService.deleteAddress(1L, 1L);

        verify(mallUserAddressMapper).updateById(any(MallUserAddressDO.class));
    }

    @Test
    void deleteAddressShouldThrowWhenNotOwner() {
        MallUserAddressDO addressDO = buildAddressDO(1L, 2L);
        when(mallUserAddressMapper.selectById(1L)).thenReturn(addressDO);

        assertThrows(BusinessException.class, () -> addressService.deleteAddress(1L, 1L));
    }

    @Test
    void setDefaultShouldSucceed() {
        MallUserAddressDO addressDO = buildAddressDO(2L, 1L);
        when(mallUserAddressMapper.selectById(2L)).thenReturn(addressDO);
        when(mallUserAddressMapper.clearDefault(1L)).thenReturn(1);
        when(mallUserAddressMapper.updateById(any(MallUserAddressDO.class))).thenReturn(1);

        addressService.setDefault(1L, 2L);

        verify(mallUserAddressMapper).clearDefault(1L);
        verify(mallUserAddressMapper).updateById(any(MallUserAddressDO.class));
    }
}

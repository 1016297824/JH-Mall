package com.mall.user.convert.response;

import com.mall.user.DO.MallUserAddressDO;
import com.mall.user.VO.AddressVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 地址 Response 转换器（DO → VO）单元测试
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
class AddressConvertTest {

    @Test
    void toAddressVOShouldMapAllFields() {
        MallUserAddressDO addressDO = new MallUserAddressDO();
        addressDO.setId(1L);
        addressDO.setReceiverName("张三");
        addressDO.setReceiverPhone("13800138000");
        addressDO.setProvince("广东省");
        addressDO.setCity("深圳市");
        addressDO.setDistrict("南山区");
        addressDO.setDetailAddress("科技园路1号");
        addressDO.setZipCode("518000");
        addressDO.setIsDefault(1);
        addressDO.setLabel("公司");

        AddressVO vo = AddressConvert.toAddressVO(addressDO);

        assertEquals("1", vo.getAddressId());
        assertEquals("张三", vo.getReceiverName());
        assertEquals("13800138000", vo.getReceiverPhone());
        assertEquals("广东省", vo.getProvince());
        assertEquals("深圳市", vo.getCity());
        assertEquals("南山区", vo.getDistrict());
        assertEquals("科技园路1号", vo.getDetailAddress());
        assertEquals("518000", vo.getZipCode());
        assertTrue(vo.getIsDefault());
        assertEquals("公司", vo.getLabel());
    }

    @Test
    void toAddressVOShouldConvertIsDefaultZeroToFalse() {
        MallUserAddressDO addressDO = new MallUserAddressDO();
        addressDO.setIsDefault(0);

        AddressVO vo = AddressConvert.toAddressVO(addressDO);

        assertFalse(vo.getIsDefault());
    }

    @Test
    void toAddressVOShouldHandleNullIsDefault() {
        MallUserAddressDO addressDO = new MallUserAddressDO();

        AddressVO vo = AddressConvert.toAddressVO(addressDO);

        assertFalse(vo.getIsDefault());
    }

    @Test
    void toAddressVOShouldConvertLongIdToString() {
        MallUserAddressDO addressDO = new MallUserAddressDO();
        addressDO.setId(123456L);

        AddressVO vo = AddressConvert.toAddressVO(addressDO);

        assertEquals("123456", vo.getAddressId());
    }
}

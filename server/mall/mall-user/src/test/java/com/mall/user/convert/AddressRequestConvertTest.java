package com.mall.user.convert;

import com.mall.user.DO.MallUserAddressDO;
import com.mall.user.VO.AddressVO;
import com.mall.user.convert.request.AddressConvert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddressRequestConvertTest {

    @Test
    void mergeShouldCopyAllFieldsToDO() {
        AddressVO vo = new AddressVO();
        vo.setReceiverName("张三");
        vo.setReceiverPhone("13800138000");
        vo.setProvince("广东省");
        vo.setCity("深圳市");
        vo.setDistrict("南山区");
        vo.setDetailAddress("科技园路1号");
        vo.setZipCode("518000");
        vo.setIsDefault(true);
        vo.setLabel("公司");

        MallUserAddressDO addressDO = new MallUserAddressDO();
        AddressConvert.merge(vo, addressDO);

        assertEquals("张三", addressDO.getReceiverName());
        assertEquals("13800138000", addressDO.getReceiverPhone());
        assertEquals("广东省", addressDO.getProvince());
        assertEquals("深圳市", addressDO.getCity());
        assertEquals("南山区", addressDO.getDistrict());
        assertEquals("科技园路1号", addressDO.getDetailAddress());
        assertEquals("518000", addressDO.getZipCode());
        assertEquals(1, addressDO.getIsDefault());
        assertEquals("公司", addressDO.getLabel());
    }

    @Test
    void mergeShouldSetIsDefaultToOneWhenTrue() {
        AddressVO vo = new AddressVO();
        vo.setIsDefault(true);

        MallUserAddressDO addressDO = new MallUserAddressDO();
        AddressConvert.merge(vo, addressDO);

        assertEquals(1, addressDO.getIsDefault());
    }

    @Test
    void mergeShouldNotSetIsDefaultWhenFalse() {
        AddressVO vo = new AddressVO();
        vo.setIsDefault(false);

        MallUserAddressDO addressDO = new MallUserAddressDO();
        addressDO.setIsDefault(1);
        AddressConvert.merge(vo, addressDO);

        assertEquals(1, addressDO.getIsDefault());
    }

    @Test
    void mergeShouldNotSetIsDefaultWhenNull() {
        AddressVO vo = new AddressVO();

        MallUserAddressDO addressDO = new MallUserAddressDO();
        addressDO.setIsDefault(1);
        AddressConvert.merge(vo, addressDO);

        assertEquals(1, addressDO.getIsDefault());
    }
}

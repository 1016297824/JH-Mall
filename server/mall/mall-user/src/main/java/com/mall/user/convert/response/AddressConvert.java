package com.mall.user.convert.response;

import com.mall.user.DO.MallUserAddressDO;
import com.mall.user.VO.AddressVO;

/**
 * 地址 DO ↔ VO 静态转换器
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public class AddressConvert {

    private AddressConvert() {
    }

    /**
     * 将地址 DO 转换为地址 VO
     *
     * @param addressDO 地址 DO
     * @return 地址 VO
     */
    public static AddressVO toAddressVO(MallUserAddressDO addressDO) {
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

package com.mall.user.convert.request;

import com.mall.user.DO.MallUserAddressDO;
import com.mall.user.VO.AddressVO;

/**
 * 地址 VO → DO 静态转换器
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public class AddressConvert {

    private AddressConvert() {
    }

    /**
     * 将地址 VO 的字段覆盖写入地址 DO（in-place）
     *
     * @param source 地址 VO
     * @param target 目标地址 DO
     */
    public static void merge(AddressVO source, MallUserAddressDO target) {
        target.setReceiverName(source.getReceiverName());
        target.setReceiverPhone(source.getReceiverPhone());
        target.setProvince(source.getProvince());
        target.setCity(source.getCity());
        target.setDistrict(source.getDistrict());
        target.setDetailAddress(source.getDetailAddress());
        target.setZipCode(source.getZipCode());
        target.setLabel(source.getLabel());
        if (source.getIsDefault() != null && source.getIsDefault()) {
            target.setIsDefault(1);
        }
    }
}

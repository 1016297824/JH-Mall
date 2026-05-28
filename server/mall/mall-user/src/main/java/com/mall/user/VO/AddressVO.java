package com.mall.user.vo;

import lombok.Data;

/**
 * 地址响应
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Data
public class AddressVO {

    /** 地址ID */
    private String addressId;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人手机号 */
    private String receiverPhone;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 详细地址 */
    private String detailAddress;

    /** 邮政编码 */
    private String zipCode;

    /** 是否默认地址 */
    private Boolean isDefault;

    /** 地址标签 */
    private String label;
}

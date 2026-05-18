package com.mall.user.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 地址簿对象 mall_user_address
 * 
 * @author ruoyi
 * @date 2026-05-18
 */
public class MallUserAddress extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增 */
    private String id;

    /** 用户 ID */
    @Excel(name = "用户 ID")
    private String userId;

    /** 收件人姓名 */
    @Excel(name = "收件人姓名")
    private String receiverName;

    /** 收件人手机号（AES-256-GCM 加密存储） */
    @Excel(name = "收件人手机号")
    private String receiverPhone;

    /** 省 */
    @Excel(name = "省")
    private String province;

    /** 市 */
    @Excel(name = "市")
    private String city;

    /** 区 */
    @Excel(name = "区")
    private String district;

    /** 详细地址 */
    @Excel(name = "详细地址")
    private String detailAddress;

    /** 邮编 */
    @Excel(name = "邮编")
    private String zipCode;

    /** 是否默认地址：1=默认 0=非默认 */
    @Excel(name = "是否默认地址：1=默认 0=非默认")
    private String isDefault;

    /** 地址标签：家 / 公司 / 学校 */
    @Excel(name = "地址标签：家 / 公司 / 学校")
    private String label;

    /** 逻辑删除标志 */
    @Excel(name = "逻辑删除标志")
    private String isDeleted;

    public void setId(String id) 
    {
        this.id = id;
    }

    public String getId() 
    {
        return id;
    }

    public void setUserId(String userId) 
    {
        this.userId = userId;
    }

    public String getUserId() 
    {
        return userId;
    }

    public void setReceiverName(String receiverName) 
    {
        this.receiverName = receiverName;
    }

    public String getReceiverName() 
    {
        return receiverName;
    }

    public void setReceiverPhone(String receiverPhone) 
    {
        this.receiverPhone = receiverPhone;
    }

    public String getReceiverPhone() 
    {
        return receiverPhone;
    }

    public void setProvince(String province) 
    {
        this.province = province;
    }

    public String getProvince() 
    {
        return province;
    }

    public void setCity(String city) 
    {
        this.city = city;
    }

    public String getCity() 
    {
        return city;
    }

    public void setDistrict(String district) 
    {
        this.district = district;
    }

    public String getDistrict() 
    {
        return district;
    }

    public void setDetailAddress(String detailAddress) 
    {
        this.detailAddress = detailAddress;
    }

    public String getDetailAddress() 
    {
        return detailAddress;
    }

    public void setZipCode(String zipCode) 
    {
        this.zipCode = zipCode;
    }

    public String getZipCode() 
    {
        return zipCode;
    }

    public void setIsDefault(String isDefault) 
    {
        this.isDefault = isDefault;
    }

    public String getIsDefault() 
    {
        return isDefault;
    }

    public void setLabel(String label) 
    {
        this.label = label;
    }

    public String getLabel() 
    {
        return label;
    }

    public void setIsDeleted(String isDeleted) 
    {
        this.isDeleted = isDeleted;
    }

    public String getIsDeleted() 
    {
        return isDeleted;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("receiverName", getReceiverName())
            .append("receiverPhone", getReceiverPhone())
            .append("province", getProvince())
            .append("city", getCity())
            .append("district", getDistrict())
            .append("detailAddress", getDetailAddress())
            .append("zipCode", getZipCode())
            .append("isDefault", getIsDefault())
            .append("label", getLabel())
            .append("isDeleted", getIsDeleted())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}

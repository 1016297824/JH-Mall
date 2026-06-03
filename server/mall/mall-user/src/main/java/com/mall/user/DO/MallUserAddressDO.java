package com.mall.user.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;

/**
 * 用户收货地址实体
 *
 * <p>对应数据库表 mall_user_address</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Data
@NoArgsConstructor
@TableName("mall_user_address")
public class MallUserAddressDO {

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 收货人姓名 */
    @TableField("receiver_name")
    private String receiverName;

    /** 收货人手机号 */
    @TableField("receiver_phone")
    private String receiverPhone;

    /** 省份 */
    @TableField("province")
    private String province;

    /** 城市 */
    @TableField("city")
    private String city;

    /** 区/县 */
    @TableField("district")
    private String district;

    /** 详细地址 */
    @TableField("detail_address")
    private String detailAddress;

    /** 邮政编码 */
    @TableField("zip_code")
    private String zipCode;

    /** 是否默认地址（0-否 1-是） */
    @TableField("is_default")
    private Integer isDefault;

    /** 地址标签（如：家、公司、学校） */
    @TableField("label")
    private String label;

    /** 逻辑删除标记（0-未删除 1-已删除） */
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}

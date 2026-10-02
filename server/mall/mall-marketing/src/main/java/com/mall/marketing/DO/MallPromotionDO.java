package com.mall.marketing.DO;

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
 * 促销活动 DO
 *
 * <p>对应表 {@code mall_marketing_promotion}，一个活动下可挂多条规则
 * （见 {@link MallPromotionRuleDO}）。</p>
 *
 * <p>{@code promotion_type} 取值见 {@code PromotionTypeEnum}
 * （1=满减 / 2=满折 / 3=免邮 / 4=秒杀），
 * {@code promotion_status} 取值见 {@code PromotionStatusEnum}
 * （0=未开始 / 1=进行中 / 2=已结束 / 3=已关闭）。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
@NoArgsConstructor
@TableName("mall_marketing_promotion")
public class MallPromotionDO {

    /** 主键，自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 活动名称 */
    @TableField("promotion_name")
    private String promotionName;

    /** 活动类型，取值见 PromotionTypeEnum */
    @TableField("promotion_type")
    private Integer promotionType;

    /** 活动开始时间 */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 活动结束时间 */
    @TableField("end_time")
    private LocalDateTime endTime;

    /** 活动状态，取值见 PromotionStatusEnum */
    @TableField("promotion_status")
    private Integer promotionStatus;

    /** 活动描述 */
    @TableField("description")
    private String description;

    /** 活动 Banner 图 URL */
    @TableField("banner_image")
    private String bannerImage;

    /** 排序值，越小越靠前 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 逻辑删除标志（0=未删，1=已删） */
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建人 */
    @TableField("create_by")
    private String createBy;

    /** 更新人 */
    @TableField("update_by")
    private String updateBy;

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

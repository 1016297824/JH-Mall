package com.mall.payment.DO;

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
 * 支付渠道配置 DO
 *
 * <p>对应表 {@code mall_payment_channel}，维护各支付渠道的启用状态与渠道参数。</p>
 *
 * <p>{@code channel_type} 取值见 {@code ChannelTypeEnum}（1=PAY / 2=REFUND）。</p>
 *
 * <p>⚠️ {@code config_json} 中含商户私钥、API Key 等敏感字段，
 * 管理端查询响应<b>不得</b>返回明文（本模块本期不含管理端接口，仅作约束记录）。</p>
 *
 * <p>该表<b>没有</b> {@code version} 列——渠道配置是低频管理端操作，
 * 不参与支付链路的并发状态推进。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Data
@NoArgsConstructor
@TableName("mall_payment_channel")
public class MallPaymentChannelDO {

    /** 主键，自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 渠道编码，如 wechat / alipay，全局唯一 */
    @TableField("channel_code")
    private String channelCode;

    /** 渠道展示名称，用于前端支付方式列表 */
    @TableField("channel_name")
    private String channelName;

    /** 渠道类型，取值见 ChannelTypeEnum */
    @TableField("channel_type")
    private Integer channelType;

    /** 渠道配置 JSON，敏感字段需加密存储 */
    @TableField("config_json")
    private String configJson;

    /** 是否启用（0=禁用，1=启用） */
    @TableField("is_enabled")
    private Integer isEnabled;

    /** 排序值，前端支付方式展示顺序 */
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

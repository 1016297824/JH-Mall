package com.mall.user.vo;

import java.util.Date;

import lombok.Data;

/**
 * 成长值流水分页条目响应
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Data
public class GrowthRecordVO {

    /** 记录ID */
    private Long id;

    /** 业务类型编码 */
    private String bizType;

    /** 业务类型名称 */
    private String bizTypeName;

    /** 变更类型：1收入/2支出 */
    private Integer changeType;

    /** 变更成长值 */
    private Integer growth;

    /** 变更前成长值 */
    private Integer beforeGrowth;

    /** 变更后成长值 */
    private Integer afterGrowth;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private Date createTime;
}

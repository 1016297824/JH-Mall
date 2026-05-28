package com.mall.user.VO;

import java.util.Date;

import lombok.Data;

/**
 * 积分流水分页条目响应
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Data
public class PointsRecordVO {

    /** 记录ID */
    private Long id;

    /** 业务类型编码 */
    private String bizType;

    /** 业务类型名称 */
    private String bizTypeName;

    /** 变更类型：1收入/2支出 */
    private Integer changeType;

    /** 变更积分 */
    private Integer points;

    /** 变更前积分 */
    private Integer beforePoints;

    /** 变更后积分 */
    private Integer afterPoints;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private Date createTime;
}

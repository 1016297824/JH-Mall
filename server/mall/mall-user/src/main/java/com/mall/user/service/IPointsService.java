package com.mall.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.common.enums.user.BizTypeEnum;
import com.mall.user.vo.PointsRecordVO;
import com.mall.user.vo.PointsVO;

/**
 * 积分服务接口
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
public interface IPointsService {

    /**
     * 查询用户积分余额
     *
     * @param userId 用户ID
     * @return 积分余额VO
     */
    PointsVO getPoints(Long userId);

    /**
     * 分页查询积分流水
     *
     * @param userId  用户ID
     * @param bizType 业务类型（可选）
     * @param page    页码
     * @param size    每页条数
     * @return 分页结果
     */
    IPage<PointsRecordVO> getPointsRecords(Long userId, String bizType, int page, int size);

    /**
     * 增加积分（乐观锁重试）
     *
     * @param userId  用户ID
     * @param points  积分数量
     * @param bizType 业务类型
     * @param bizNo   业务单号
     */
    void addPoints(Long userId, int points, BizTypeEnum bizType, String bizNo);
}

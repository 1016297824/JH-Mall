package com.mall.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.user.vo.GrowthRecordVO;

/**
 * 成长值流水服务接口 — 仅操作 mall_user_growth_log 表
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
public interface IGrowthService {

    /**
     * 分页查询成长值流水
     *
     * @param userId  用户ID
     * @param bizType 业务类型（可选）
     * @param page    页码
     * @param size    每页条数
     * @return 分页结果
     */
    IPage<GrowthRecordVO> getGrowthRecords(Long userId, String bizType, int page, int size);
}

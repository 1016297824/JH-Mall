package com.mall.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.common.enums.user.BizTypeEnum;
import com.mall.user.VO.GrowthRecordVO;
import com.mall.user.VO.GrowthVO;
import com.mall.user.VO.MembershipVO;

/**
 * 会员服务接口
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
public interface IMemberService {

    /**
     * 查询会员信息
     *
     * @param userId 用户ID
     * @return 会员信息VO
     */
    MembershipVO getMembership(Long userId);

    /**
     * 查询成长值
     *
     * @param userId 用户ID
     * @return 成长值VO
     */
    GrowthVO getGrowth(Long userId);

    /**
     * 增加成长值
     *
     * @param userId  用户ID
     * @param growth  成长值
     * @param bizType 业务类型
     * @param bizNo   业务单号
     */
    void addGrowth(Long userId, int growth, BizTypeEnum bizType, String bizNo);

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

package com.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.user.DO.MallUserMemberDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户会员信息 Mapper
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@Mapper
public interface MallUserMemberMapper extends BaseMapper<MallUserMemberDO> {

    /**
     * 原子增加成长值
     *
     * @param userId  用户ID
     * @param growth  成长值增量
     * @return 影响行数
     */
    @Update("update mall_user_member set growth = growth + #{growth}, total_growth = total_growth + #{growth}, update_time = now() where user_id = #{userId}")
    int addGrowth(@Param("userId") Long userId, @Param("growth") Integer growth);

    /**
     * 更新会员等级
     *
     * @param userId  用户ID
     * @param levelId 会员等级ID
     * @return 影响行数
     */
    @Update("update mall_user_member set level_id = #{levelId}, level_start_time = now(), update_time = now() where user_id = #{userId}")
    int updateLevel(@Param("userId") Long userId, @Param("levelId") Long levelId);
}

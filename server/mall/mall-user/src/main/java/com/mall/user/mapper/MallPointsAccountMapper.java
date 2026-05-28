package com.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.user.DO.MallPointsAccountDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 积分账户 Mapper
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@Mapper
public interface MallPointsAccountMapper extends BaseMapper<MallPointsAccountDO> {

    /**
     * 原子增加积分（乐观锁）
     *
     * @param userId  用户ID
     * @param points  积分增量
     * @param version 当前版本号
     * @return 影响行数（0表示版本冲突）
     */
    @Update("update mall_user_points_account set available_points = available_points + #{points}, total_points = total_points + #{points}, version = version + 1, update_time = now() where user_id = #{userId} and version = #{version}")
    int addPoints(@Param("userId") Long userId, @Param("points") Integer points, @Param("version") Integer version);

    /**
     * 将所有可用积分移至过期（分批清零用）
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Update("update mall_user_points_account set expired_points = expired_points + available_points, available_points = 0, version = version + 1, update_time = now() where user_id = #{userId} and available_points > 0")
    int expirePoints(@Param("userId") Long userId);
}

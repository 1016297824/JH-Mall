package com.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.user.DO.MallUserPointsLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户积分日志 Mapper
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Mapper
public interface MallUserPointsLogMapper extends BaseMapper<MallUserPointsLogDO> {

    /**
     * 回填流水的变动前后余额快照
     *
     * <p>流水行在改余额<b>之前</b>就已插入（用于抢占唯一键做幂等锚点），
     * 此时还读不到余额，故更新成功后再回填这两个展示字段。</p>
     *
     * @param id           流水 ID
     * @param beforePoints 变动前可用积分
     * @param afterPoints  变动后可用积分
     * @return 影响行数
     */
    @Update("UPDATE mall_user_points_log SET before_points = #{beforePoints}, "
            + "after_points = #{afterPoints}, update_time = NOW() WHERE id = #{id}")
    int updateSnapshot(@Param("id") Long id,
                       @Param("beforePoints") int beforePoints,
                       @Param("afterPoints") int afterPoints);
}

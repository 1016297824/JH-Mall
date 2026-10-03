package com.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.user.DO.MallUserGrowthLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户成长值日志 Mapper
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Mapper
public interface MallUserGrowthLogMapper extends BaseMapper<MallUserGrowthLogDO> {

    /**
     * 回填流水的变动前后余额快照
     *
     * <p>流水行在改成长值<b>之前</b>就已插入（用于抢占唯一键做幂等锚点），
     * 此时还读不到余额，故更新成功后再回填这两个展示字段。</p>
     *
     * @param id           流水 ID
     * @param beforeGrowth 变动前成长值
     * @param afterGrowth  变动后成长值
     * @return 影响行数
     */
    @Update("UPDATE mall_user_growth_log SET before_growth = #{beforeGrowth}, "
            + "after_growth = #{afterGrowth}, update_time = NOW() WHERE id = #{id}")
    int updateSnapshot(@Param("id") Long id,
                       @Param("beforeGrowth") int beforeGrowth,
                       @Param("afterGrowth") int afterGrowth);
}

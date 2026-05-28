package com.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.user.DO.MallUserPointsLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户积分日志 Mapper
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Mapper
public interface MallUserPointsLogMapper extends BaseMapper<MallUserPointsLogDO> {

    /**
     * 分页查询用户积分流水
     *
     * @param page    分页对象
     * @param userId  用户ID
     * @param bizType 业务类型
     * @return 分页结果
     */
    @Select("<script>select id, user_id, biz_type, biz_no, change_type, points, before_points, after_points, remark, create_time from mall_user_points_log where user_id = #{userId}<if test='bizType != null and bizType != \"\"'> and biz_type = #{bizType}</if> order by create_time desc</script>")
    IPage<MallUserPointsLogDO> selectByUserIdPage(Page<MallUserPointsLogDO> page, @Param("userId") Long userId, @Param("bizType") String bizType);
}

package com.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.user.DO.MallUserGrowthLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户成长值日志 Mapper
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Mapper
public interface MallUserGrowthLogMapper extends BaseMapper<MallUserGrowthLogDO> {

    /**
     * 分页查询用户成长值流水
     *
     * @param page    分页对象
     * @param userId  用户ID
     * @param bizType 业务类型
     * @return 分页结果
     */
    @Select("<script>select id, user_id, biz_type, biz_no, change_type, growth, before_growth, after_growth, remark, create_time from mall_user_growth_log where user_id = #{userId}<if test='bizType != null and bizType != \"\"'> and biz_type = #{bizType}</if> order by create_time desc</script>")
    IPage<MallUserGrowthLogDO> selectByUserIdPage(Page<MallUserGrowthLogDO> page, @Param("userId") Long userId, @Param("bizType") String bizType);
}

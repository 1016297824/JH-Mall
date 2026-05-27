package com.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.user.DO.MallUserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户账号 Mapper
 *
 * <p>使用 MyBatis-Plus BaseMapper 提供的 CRUD 方法，通过 Wrapper 构建查询条件</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@Mapper
public interface MallUserMapper extends BaseMapper<MallUserDO> {
}

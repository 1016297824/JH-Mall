package com.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.user.DO.MallUserMemberDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户会员信息 Mapper
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@Mapper
public interface MallUserMemberMapper extends BaseMapper<MallUserMemberDO> {
}

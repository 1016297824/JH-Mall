package com.mall.user.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    /**
     * 根据手机号哈希查询用户
     *
     * @param phoneHash 手机号 SHA-256 哈希
     * @return 用户实体，未找到返回 null
     */
    default MallUserDO selectByPhoneHash(String phoneHash) {
        return selectOne(new LambdaQueryWrapper<MallUserDO>()
                .eq(MallUserDO::getPhoneHash, phoneHash)
                .eq(MallUserDO::getIsDeleted, 0));
    }
}

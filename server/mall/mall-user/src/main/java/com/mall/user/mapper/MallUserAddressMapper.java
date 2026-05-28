package com.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.user.DO.MallUserAddressDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户收货地址 Mapper
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@Mapper
public interface MallUserAddressMapper extends BaseMapper<MallUserAddressDO> {

    /**
     * 清除用户所有默认地址标记
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Update("update mall_user_address set is_default = 0, update_time = now() where user_id = #{userId}")
    int clearDefault(@Param("userId") Long userId);
}

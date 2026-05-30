package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.DO.MallBrandDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 品牌 Mapper
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Mapper
public interface MallBrandMapper extends BaseMapper<MallBrandDO> {

    /**
     * 查询全部未删除品牌（按排序值升序）
     *
     * @return 品牌列表
     */
    default List<MallBrandDO> selectAll() {
        return selectList(new LambdaQueryWrapper<MallBrandDO>()
                .eq(MallBrandDO::getIsDeleted, 0)
                .orderByAsc(MallBrandDO::getSortOrder));
    }
}

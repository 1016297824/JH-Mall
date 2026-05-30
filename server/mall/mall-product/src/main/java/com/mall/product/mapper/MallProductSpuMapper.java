package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.product.DO.MallProductSpuDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * SPU Mapper
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Mapper
public interface MallProductSpuMapper extends BaseMapper<MallProductSpuDO> {

    /**
     * 分页查询已上架 SPU（含分类/品牌/关键词过滤）
     *
     * @param page       分页参数
     * @param categoryId 类目 ID（可选）
     * @param brandId    品牌 ID（可选）
     * @param keyword    关键词（可选）
     * @return SPU 分页结果
     */
    default Page<MallProductSpuDO> selectPublishedPage(Page<MallProductSpuDO> page,
                                                        Long categoryId, Long brandId, String keyword) {
        LambdaQueryWrapper<MallProductSpuDO> wrapper = new LambdaQueryWrapper<MallProductSpuDO>()
                .eq(MallProductSpuDO::getPublishStatus, 1)
                .eq(MallProductSpuDO::getVerifyStatus, 1)
                .eq(MallProductSpuDO::getIsDeleted, 0);

        if (categoryId != null) {
            wrapper.eq(MallProductSpuDO::getCategoryId, categoryId);
        }
        if (brandId != null) {
            wrapper.eq(MallProductSpuDO::getBrandId, brandId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(MallProductSpuDO::getSpuName, keyword);
        }

        return selectPage(page, wrapper);
    }

    /**
     * 分页查询全部未删除 SPU（供全量重建使用）
     *
     * @param page 分页参数
     * @return SPU 分页结果
     */
    default Page<MallProductSpuDO> selectAllPage(Page<MallProductSpuDO> page) {
        LambdaQueryWrapper<MallProductSpuDO> wrapper = new LambdaQueryWrapper<MallProductSpuDO>()
                .eq(MallProductSpuDO::getIsDeleted, 0);
        return selectPage(page, wrapper);
    }
}

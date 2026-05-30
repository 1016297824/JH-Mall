package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.product.DO.MallProductSpuDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MallProductSpuMapper extends BaseMapper<MallProductSpuDO> {

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

    default Page<MallProductSpuDO> selectAllPage(Page<MallProductSpuDO> page) {
        LambdaQueryWrapper<MallProductSpuDO> wrapper = new LambdaQueryWrapper<MallProductSpuDO>()
                .eq(MallProductSpuDO::getIsDeleted, 0);
        return selectPage(page, wrapper);
    }
}

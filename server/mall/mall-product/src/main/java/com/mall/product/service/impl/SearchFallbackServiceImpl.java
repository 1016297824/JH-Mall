package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.product.DO.MallProductSpuDO;
import com.mall.product.VO.SpuVO;
import com.mall.product.convert.response.SpuConvert;
import com.mall.product.mapper.MallProductSpuMapper;
import com.mall.product.service.ISearchFallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchFallbackServiceImpl implements ISearchFallbackService {

    private final MallProductSpuMapper mallProductSpuMapper;

    @Override
    public List<SpuVO> search(String keyword, int page, int size) {
        if (keyword.length() < 2 || keyword.length() > 50) {
            throw new BusinessException(ErrorCode.SEARCH_RESULT_LIMIT);
        }
        if (size > 100) {
            size = 100;
        }

        Page<MallProductSpuDO> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MallProductSpuDO> wrapper = new LambdaQueryWrapper<MallProductSpuDO>()
                .eq(MallProductSpuDO::getPublishStatus, 1)
                .eq(MallProductSpuDO::getIsDeleted, 0)
                .like(MallProductSpuDO::getSpuName, keyword);

        Page<MallProductSpuDO> result = mallProductSpuMapper.selectPage(pageParam, wrapper);

        return result.getRecords().stream()
                .map(SpuConvert::toSpuVO)
                .toList();
    }
}

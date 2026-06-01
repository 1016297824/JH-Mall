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

/**
 * 搜索降级服务实现
 *
 * <p>搜索引擎不可用时，退化为数据库 SPU 名称 LIKE 模糊查询</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchFallbackServiceImpl implements ISearchFallbackService {

    private final MallProductSpuMapper mallProductSpuMapper;

    @Override
    public List<SpuVO> search(String keyword, int page, int size) {
        // 关键词长度校验（2~50 字符），过短或过长都直接返回参数错误
        if (keyword.length() < 2 || keyword.length() > 50) {
            throw new BusinessException(ErrorCode.SEARCH_RESULT_LIMIT);
        }
        // 单页上限 100，防止模糊查询被大页数打垮 DB
        if (size > 100) {
            size = 100;
        }
        // 构建查询条件：已上架 + 未逻辑删除 + SPU 名称 LIKE 模糊匹配
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

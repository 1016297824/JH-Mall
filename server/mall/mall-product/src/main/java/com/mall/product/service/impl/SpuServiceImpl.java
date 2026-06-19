package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.DTO.PageResult;
import com.mall.common.DTO.product.SpuDTO;
import com.mall.common.DTO.product.SpuSearchDTO;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.product.DO.MallBrandDO;
import com.mall.product.DO.MallCategoryDO;
import com.mall.product.DO.MallProductSkuDO;
import com.mall.product.DO.MallProductSpuDO;
import com.mall.product.VO.SpuDetailVO;
import com.mall.product.VO.SpuVO;
import com.mall.product.convert.response.SpuConvert;
import com.mall.product.mapper.MallBrandMapper;
import com.mall.product.mapper.MallCategoryMapper;
import com.mall.product.mapper.MallProductSkuMapper;
import com.mall.product.mapper.MallProductSpuMapper;
import com.mall.product.service.IHotProductService;
import com.mall.product.service.ISpuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * SPU 服务实现
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpuServiceImpl implements ISpuService {

    private static final int SUB_TITLE_MAX_LENGTH = 200;

    private final MallProductSpuMapper mallProductSpuMapper;
    private final MallProductSkuMapper mallProductSkuMapper;
    private final MallBrandMapper mallBrandMapper;
    private final MallCategoryMapper mallCategoryMapper;
    private final IHotProductService hotProductService;

    @Override
    public PageResult<SpuVO> page(int page, int size, Long categoryId, Long brandId, String keyword, String sort) {
        Page<MallProductSpuDO> pageParam = new Page<>(page, size);
        applySort(pageParam, sort);
        Page<MallProductSpuDO> result = mallProductSpuMapper.selectPublishedPage(pageParam, categoryId, brandId, keyword);
        List<SpuVO> voList = result.getRecords().stream().map(SpuConvert::toSpuVO).toList();
        fillBrandNames(voList);
        return PageResult.of(page, size, result.getTotal(), voList);
    }

    /**
     * 批量填充品牌名（按 brandId 批量查找，避免 N+1）
     */
    private void fillBrandNames(List<SpuVO> voList) {
        if (voList.isEmpty()) return;
        List<Long> brandIds = voList.stream()
                .map(SpuVO::getBrandId)
                .filter(Objects::nonNull)
                .map(Long::valueOf)
                .distinct()
                .toList();
        if (brandIds.isEmpty()) return;
        List<MallBrandDO> brands = mallBrandMapper.selectBatchIds(brandIds);
        Map<Long, String> nameMap = new HashMap<>();
        for (MallBrandDO b : brands) {
            if (b != null) nameMap.put(b.getId(), b.getName());
        }
        for (SpuVO vo : voList) {
            if (vo.getBrandId() != null) {
                vo.setBrandName(nameMap.get(Long.valueOf(vo.getBrandId())));
            }
        }
    }

    @Override
    public SpuDetailVO detail(Long spuId) {
        MallProductSpuDO spuDO = mallProductSpuMapper.selectById(spuId);
        // 校验 SPU 存在性：记录存在、未逻辑删除、已上架、已通过审核，四项全满足才可访问
        if (spuDO == null
                || Integer.valueOf(1).equals(spuDO.getIsDeleted())
                || Integer.valueOf(1).equals(spuDO.getPublishStatus()) == false
                || Integer.valueOf(1).equals(spuDO.getVerifyStatus()) == false) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        // 查询关联 SKU 列表（未删除 SKU）
        List<MallProductSkuDO> skuDOList = mallProductSkuMapper.selectBySpuId(spuId);
        // 记录 UV（HyperLogLog），每次详情访问算一个独立访客，未登录传 0L
        hotProductService.incrUv(spuId, 0L);
        SpuDetailVO detailVO = SpuConvert.toSpuDetailVO(spuDO, skuDOList);
        fillBrandName(detailVO);
        return detailVO;
    }

    @Override
    public PageResult<SpuDTO> pageForFullRebuild(int page, int size) {
        // 分页查询全部未删除 SPU（不分上架状态）
        Page<MallProductSpuDO> pageParam = new Page<>(page, size);
        Page<MallProductSpuDO> result = mallProductSpuMapper.selectAllPage(pageParam);
        List<SpuDTO> dtoList = result.getRecords().stream().map(this::toSpuDTO).toList();
        return PageResult.of(page, size, result.getTotal(), dtoList);
    }

    @Override
    public PageResult<SpuSearchDTO> pageForSearchRebuild(int page, int size) {
        // 分页查询全部未删除 SPU
        Page<MallProductSpuDO> pageParam = new Page<>(page, size);
        Page<MallProductSpuDO> result = mallProductSpuMapper.selectAllPage(pageParam);
        List<MallProductSpuDO> spuDOList = result.getRecords();
        if (spuDOList.isEmpty()) {
            return PageResult.of(page, size, 0, List.of());
        }
        // 批量预取：类目名称、品牌名称、SKU 列表（N+1 优化，3 次批量查询替代 3N 次单条查询）
        Map<Long, String> categoryNameMap = buildCategoryNameMap(spuDOList);
        Map<Long, String> brandNameMap = buildBrandNameMap(spuDOList);
        Map<Long, List<MallProductSkuDO>> skuMap = buildSkuMapBySpuIds(spuDOList);
        List<SpuSearchDTO> dtoList = spuDOList.stream()
                .map(spuDO -> toSpuSearchDTO(spuDO, categoryNameMap, brandNameMap, skuMap))
                .toList();
        return PageResult.of(page, size, result.getTotal(), dtoList);
    }

    @Override
    public List<SpuVO> hotList(int limit) {
        List<SpuVO> hotList = hotProductService.hotList(limit);
        fillBrandNames(hotList);
        return hotList;
    }

    /**
     * 填充单个 VO 的品牌名
     */
    private void fillBrandName(SpuVO vo) {
        if (vo == null || vo.getBrandId() == null) return;
        MallBrandDO brand = mallBrandMapper.selectById(Long.valueOf(vo.getBrandId()));
        if (brand != null) {
            vo.setBrandName(brand.getName());
        }
    }

    /**
     * SPU DO 转 SpuDTO（全量重建用）
     */
    private SpuDTO toSpuDTO(MallProductSpuDO spuDO) {
        SpuDTO dto = new SpuDTO();
        dto.setSpuId(spuDO.getId());
        dto.setSpuName(spuDO.getSpuName());
        dto.setMainImage(spuDO.getMainImage());
        dto.setPriceMin(spuDO.getPriceMin());
        dto.setPriceMax(spuDO.getPriceMax());
        dto.setPublishStatus(spuDO.getPublishStatus());
        dto.setSalesCount(spuDO.getSalesCount());
        dto.setCategoryId(spuDO.getCategoryId());
        dto.setBrandId(spuDO.getBrandId());
        return dto;
    }

    /**
     * SPU DO 转 SpuSearchDTO（接收预计算 Map，避免 N+1 查询）
     *
     * @param spuDO           SPU DO
     * @param categoryNameMap 类目 ID → 类目名称
     * @param brandNameMap    品牌 ID → 品牌名称
     * @param skuMap          SPU ID → SKU 列表
     * @return SpuSearchDTO
     */
    private SpuSearchDTO toSpuSearchDTO(MallProductSpuDO spuDO,
                                         Map<Long, String> categoryNameMap,
                                         Map<Long, String> brandNameMap,
                                         Map<Long, List<MallProductSkuDO>> skuMap) {
        SpuSearchDTO dto = new SpuSearchDTO();
        dto.setSpuId(spuDO.getId());
        dto.setSpuName(spuDO.getSpuName());
        // 副标题：spu_description 截断 200 字
        if (spuDO.getSpuDescription() != null) {
            dto.setSubTitle(spuDO.getSpuDescription().length() > SUB_TITLE_MAX_LENGTH
                    ? spuDO.getSpuDescription().substring(0, SUB_TITLE_MAX_LENGTH) : spuDO.getSpuDescription());
        }
        dto.setMainImage(spuDO.getMainImage());
        dto.setPriceMin(spuDO.getPriceMin());
        dto.setPublishStatus(spuDO.getPublishStatus());
        dto.setSalesCount(spuDO.getSalesCount());
        dto.setCategoryId(spuDO.getCategoryId());
        dto.setBrandId(spuDO.getBrandId());
        dto.setCreateTime(spuDO.getCreateTime());
        dto.setUpdateTime(spuDO.getUpdateTime());
        // 类目名称（从预取 Map 获取）
        dto.setCategoryName(categoryNameMap.get(spuDO.getCategoryId()));
        // 品牌名称（从预取 Map 获取）
        dto.setBrandName(brandNameMap.get(spuDO.getBrandId()));
        // SKU 规格拼接（从预取 Map 获取）
        List<MallProductSkuDO> skus = skuMap.getOrDefault(spuDO.getId(), List.of());
        if (!skus.isEmpty()) {
            dto.setSpuSpecs(skus.stream()
                    .map(s -> s.getAttrsJson() != null ? s.getAttrsJson() : "")
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(" ")));
        }
        return dto;
    }

    /**
     * 批量构建类目 ID → 名称映射
     */
    private Map<Long, String> buildCategoryNameMap(List<MallProductSpuDO> spuDOList) {
        List<Long> categoryIds = spuDOList.stream()
                .map(MallProductSpuDO::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (categoryIds.isEmpty()) return Map.of();
        List<MallCategoryDO> categories = mallCategoryMapper.selectBatchIds(categoryIds);
        Map<Long, String> nameMap = new HashMap<>();
        for (MallCategoryDO cat : categories) {
            if (cat != null) nameMap.put(cat.getId(), cat.getName());
        }
        return nameMap;
    }

    /**
     * 批量构建品牌 ID → 名称映射
     */
    private Map<Long, String> buildBrandNameMap(List<MallProductSpuDO> spuDOList) {
        List<Long> brandIds = spuDOList.stream()
                .map(MallProductSpuDO::getBrandId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (brandIds.isEmpty()) return Map.of();
        List<MallBrandDO> brands = mallBrandMapper.selectBatchIds(brandIds);
        Map<Long, String> nameMap = new HashMap<>();
        for (MallBrandDO brand : brands) {
            if (brand != null) nameMap.put(brand.getId(), brand.getName());
        }
        return nameMap;
    }

    /**
     * 批量构建 SPU ID → SKU 列表映射
     */
    private Map<Long, List<MallProductSkuDO>> buildSkuMapBySpuIds(List<MallProductSpuDO> spuDOList) {
        List<Long> spuIds = spuDOList.stream()
                .map(MallProductSpuDO::getId)
                .distinct()
                .toList();
        if (spuIds.isEmpty()) return Map.of();
        List<MallProductSkuDO> skus = mallProductSkuMapper.batchSelectBySpuIds(spuIds);
        return skus.stream().collect(Collectors.groupingBy(MallProductSkuDO::getSpuId));
    }

    /**
     * 应用排序参数
     *
     * @param pageParam 分页参数
     * @param sort      排序方式
     */
    private void applySort(Page<MallProductSpuDO> pageParam, String sort) {
        if ("price_asc".equals(sort)) {
            pageParam.addOrder(OrderItem.asc("price_min"));
        } else if ("price_desc".equals(sort)) {
            pageParam.addOrder(OrderItem.desc("price_min"));
        } else if ("sales_desc".equals(sort)) {
            pageParam.addOrder(OrderItem.desc("sales_count"));
        }
    }
}

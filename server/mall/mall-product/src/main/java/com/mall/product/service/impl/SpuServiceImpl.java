package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.DTO.PageResult;
import com.mall.common.DTO.product.SpuDTO;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.product.DO.MallBrandDO;
import com.mall.product.DO.MallProductSkuDO;
import com.mall.product.DO.MallProductSpuDO;
import com.mall.product.VO.SpuDetailVO;
import com.mall.product.VO.SpuVO;
import com.mall.product.convert.response.SpuConvert;
import com.mall.product.mapper.MallBrandMapper;
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

    private final MallProductSpuMapper mallProductSpuMapper;
    private final MallProductSkuMapper mallProductSkuMapper;
    private final MallBrandMapper mallBrandMapper;
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

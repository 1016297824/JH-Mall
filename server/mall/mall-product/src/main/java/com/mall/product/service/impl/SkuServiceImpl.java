package com.mall.product.service.impl;

import com.mall.common.DTO.product.ProductSkuDTO;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.product.DO.MallProductSkuDO;
import com.mall.product.DO.MallProductSpuDO;
import com.mall.product.DO.MallSkuStockDO;
import com.mall.product.VO.SkuVO;
import com.mall.product.convert.response.SkuConvert;
import com.mall.product.mapper.MallProductSkuMapper;
import com.mall.product.mapper.MallProductSpuMapper;
import com.mall.product.mapper.MallSkuStockMapper;
import com.mall.product.service.ISkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * SKU 服务实现
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkuServiceImpl implements ISkuService {

    private final MallProductSkuMapper mallProductSkuMapper;
    private final MallSkuStockMapper mallSkuStockMapper;
    private final MallProductSpuMapper mallProductSpuMapper;

    @Override
    public SkuVO getBySkuId(Long skuId) {
        // 查询 SKU 基本信息
        MallProductSkuDO skuDO = mallProductSkuMapper.selectBySkuId(skuId);
        if (skuDO == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        SkuVO vo = SkuConvert.toSkuVO(skuDO);
        // 补充可用库存信息
        MallSkuStockDO stock = mallSkuStockMapper.selectBySkuId(skuId);
        if (stock != null) {
            vo.setAvailableStock(stock.getAvailableStock());
        }
        return vo;
    }

    @Override
    public List<ProductSkuDTO> batchGetSkuDTOs(List<Long> skuIds) {
        // 批量查询 SKU 基本信息（含逻辑删除过滤）
        List<MallProductSkuDO> skuDOList = mallProductSkuMapper.selectBySkuIds(skuIds);
        // 批量查询库存并构建 skuId → stock 映射，避免 N+1 查询
        Map<Long, MallSkuStockDO> stockMap = mallSkuStockMapper.selectBySkuIds(skuIds).stream()
                .collect(Collectors.toMap(MallSkuStockDO::getSkuId, s -> s));
        // 批量查询 SPU 名称：下单需要写入 mall_order_item.spu_name（NOT NULL）快照
        Map<Long, String> spuNameMap = loadSpuNames(skuDOList);

        // 逐条组装 DTO，可用库存 > 0 标记为在售状态
        return skuDOList.stream().map(sku -> {
            ProductSkuDTO dto = new ProductSkuDTO();
            dto.setSkuId(sku.getId());
            dto.setSpuId(sku.getSpuId());
            dto.setSkuCode(sku.getSkuCode());
            dto.setSkuName(sku.getSkuName());
            dto.setSpuName(spuNameMap.get(sku.getSpuId()));
            dto.setPrice(sku.getPrice());
            dto.setImage(sku.getImage());
            MallSkuStockDO stock = stockMap.get(sku.getId());
            // 可用库存大于 0 且库存记录存在才算在售
            dto.setIsOnSale(stock != null && stock.getAvailableStock() > 0);
            dto.setAvailableQty(stock != null ? stock.getAvailableStock() : 0);
            return dto;
        }).toList();
    }

    /**
     * 批量查询 SKU 所属 SPU 的名称
     *
     * <p>SPU 已被删除时对应名称缺失，DTO 该字段为 null，由调用方决定兜底策略。</p>
     *
     * @param skuDOList SKU 列表
     * @return spuId → spuName 映射
     */
    private Map<Long, String> loadSpuNames(List<MallProductSkuDO> skuDOList) {
        List<Long> spuIds = skuDOList.stream()
                .map(MallProductSkuDO::getSpuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (spuIds.isEmpty()) {
            // selectBatchIds 传空集合会生成非法 SQL，必须先短路
            return Collections.emptyMap();
        }
        return mallProductSpuMapper.selectBatchIds(spuIds).stream()
                .collect(Collectors.toMap(MallProductSpuDO::getId, MallProductSpuDO::getSpuName,
                        (existing, ignored) -> existing));
    }
}

package com.mall.product.service.impl;

import com.mall.common.DTO.product.ProductSkuDTO;
import com.mall.common.enums.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.product.DO.MallProductSkuDO;
import com.mall.product.DO.MallSkuStockDO;
import com.mall.product.VO.SkuVO;
import com.mall.product.convert.response.SkuConvert;
import com.mall.product.mapper.MallProductSkuMapper;
import com.mall.product.mapper.MallSkuStockMapper;
import com.mall.product.service.ISkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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
        // 批量查询 SKU 基本信息
        List<MallProductSkuDO> skuDOList = mallProductSkuMapper.selectBySkuIds(skuIds);
        // 批量查询库存，建立 skuId → stock 映射
        Map<Long, MallSkuStockDO> stockMap = mallSkuStockMapper.selectBySkuIds(skuIds).stream()
                .collect(Collectors.toMap(MallSkuStockDO::getSkuId, s -> s));

        // 组装 DTO，根据库存判断是否在售
        return skuDOList.stream().map(sku -> {
            ProductSkuDTO dto = new ProductSkuDTO();
            dto.setSkuId(sku.getId());
            dto.setSpuId(sku.getSpuId());
            dto.setSkuCode(sku.getSkuCode());
            dto.setSkuName(sku.getSkuName());
            dto.setPrice(sku.getPrice());
            dto.setImage(sku.getImage());
            MallSkuStockDO stock = stockMap.get(sku.getId());
            dto.setIsOnSale(stock != null && stock.getAvailableStock() > 0);
            dto.setAvailableQty(stock != null ? stock.getAvailableStock() : 0);
            return dto;
        }).toList();
    }
}

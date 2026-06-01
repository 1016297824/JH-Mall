package com.mall.product.service;

import com.mall.common.DTO.PageResult;
import com.mall.common.DTO.product.SpuDTO;
import com.mall.product.VO.SpuDetailVO;
import com.mall.product.VO.SpuVO;

import java.util.List;

/**
 * SPU 服务接口
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public interface ISpuService {

    /**
     * 分页查询已上架 SPU
     *
     * @param page      页码（从 1 开始）
     * @param size      每页条数
     * @param categoryId 类目 ID（可选）
     * @param brandId   品牌 ID（可选）
     * @param keyword   关键词（可选，模糊匹配 SPU 名称）
     * @param sort      排序方式（price_asc/price_desc/sales_desc）
     * @return 分页结果
     */
    PageResult<SpuVO> page(int page, int size, Long categoryId, Long brandId, String keyword, String sort);

    /**
     * 获取 SPU 详情（含 SKU 列表）
     *
     * @param spuId SPU ID
     * @return SPU 详情
     */
    SpuDetailVO detail(Long spuId);

    /**
     * 全量重建分页查询（供搜索索引重建使用）
     *
     * @param page 页码
     * @param size 每页条数
     * @return SPU DTO 分页
     */
    PageResult<SpuDTO> pageForFullRebuild(int page, int size);

    /**
     * 获取热点商品列表
     *
     * @param limit 返回条数（最大 50）
     * @return 热度降序 SpuVO 列表
     */
    List<SpuVO> hotList(int limit);
}

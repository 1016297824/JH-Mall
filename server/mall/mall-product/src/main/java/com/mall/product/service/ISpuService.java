package com.mall.product.service;

import com.mall.common.DTO.PageResult;
import com.mall.common.DTO.product.SpuDTO;
import com.mall.common.DTO.product.SpuSearchDTO;
import com.mall.product.VO.SpuDetailVO;
import com.mall.product.VO.SpuVO;

import java.time.LocalDateTime;
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
     * 全量分页查询 SPU（搜索索引重建专用）
     *
     * <p>返回含类目名、品牌名、SKU 规格拼接的富 DTO，供 mall-search 全量重建使用</p>
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     * @return SpuSearchDTO 分页结果
     */
    PageResult<SpuSearchDTO> pageForSearchRebuild(int page, int size);

    /**
     * 分页查询指定时刻之后有变更的 SPU（搜索索引重建的增量回补专用）
     *
     * <p>全量重建是「先切别名再分批灌数」，灌数期间发生的商品变更可能被随后到达的
     * 旧快照批量覆盖（读第 N 页 → 商品改价 → 写第 N 页），且该变更已被实时同步消费掉，
     * 无人会再修正。故全量灌完后按 {@code update_time >= since} 重扫一遍补齐。</p>
     *
     * @param since 重建开始时刻
     * @param page  页码（从 1 开始）
     * @param size  每页条数
     * @return SpuSearchDTO 分页结果
     */
    PageResult<SpuSearchDTO> pageForSearchRebuildSince(LocalDateTime since, int page, int size);

    /**
     * 按 spuId 查询单条搜索索引 DTO（增量同步专用）
     *
     * <p>替代 {@code pageForSearchRebuild} 的全表分页扫描：增量同步只需一条商品，
     * 原先要逐页拉全量来比对，数据量大时单次同步 O(N)。</p>
     *
     * @param spuId SPU ID
     * @return SpuSearchDTO；商品不存在返回 null
     */
    SpuSearchDTO getForSearchRebuild(Long spuId);

    /**
     * 获取热点商品列表
     *
     * @param limit 返回条数（最大 50）
     * @return 热度降序 SpuVO 列表
     */
    List<SpuVO> hotList(int limit);
}

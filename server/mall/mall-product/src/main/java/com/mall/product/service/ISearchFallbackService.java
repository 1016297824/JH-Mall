package com.mall.product.service;

import com.mall.product.VO.SpuVO;
import java.util.List;

/**
 * 搜索降级服务接口
 *
 * <p>当搜索引擎不可用时，走数据库 LIKE 模糊查询作为降级方案</p>
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public interface ISearchFallbackService {

    /**
     * 降级搜索（数据库 LIKE 查询）
     *
     * @param keyword 搜索关键词
     * @param page    页码
     * @param size    每页条数
     * @return SPU 列表
     */
    List<SpuVO> search(String keyword, int page, int size);
}

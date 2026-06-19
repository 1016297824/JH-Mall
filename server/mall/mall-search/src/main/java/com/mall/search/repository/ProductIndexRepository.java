package com.mall.search.repository;

import com.mall.search.DO.ProductIndexDO;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 商品搜索索引 Repository
 *
 * <p>继承 ElasticsearchRepository 获得基础的 CRUD 能力（save/saveAll/deleteById/findById）</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
@Repository
public interface ProductIndexRepository extends ElasticsearchRepository<ProductIndexDO, Long> {
}

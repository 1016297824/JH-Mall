package com.mall.search.convert.request;

import com.mall.common.DTO.product.SpuSearchDTO;
import com.mall.search.DO.ProductIndexDO;
import org.springframework.data.elasticsearch.core.suggest.Completion;

/**
 * SpuSearchDTO -> ProductIndexDO 转换器
 *
 * <p>全量重建 / 增量同步时使用，将 mall-product 返回的搜索专用 DTO 转为 ES 索引实体。</p>
 *
 * @author JH-Mall
 * @date 2026/06/19
 */
public final class SpuSearchConvert {

    private SpuSearchConvert() {
    }

    /**
     * 将 SpuSearchDTO 转换为 ProductIndexDO
     *
     * @param dto 搜索专用 SPU DTO
     * @return ES 索引实体，dto 为 null 时返回 null
     */
    public static ProductIndexDO toProductIndex(SpuSearchDTO dto) {
        if (dto == null) {
            return null;
        }
        ProductIndexDO indexDO = new ProductIndexDO();
        indexDO.setProductId(dto.getSpuId());
        indexDO.setSpuName(dto.getSpuName());
        indexDO.setSubTitle(dto.getSubTitle());
        indexDO.setImage(dto.getMainImage());
        // priceMin 为 Long（分），需转为 Integer
        indexDO.setPrice(dto.getPriceMin() != null ? dto.getPriceMin().intValue() : null);
        indexDO.setSalesCount(dto.getSalesCount());
        indexDO.setCategoryId(dto.getCategoryId());
        indexDO.setCategoryName(dto.getCategoryName());
        indexDO.setBrandId(dto.getBrandId());
        indexDO.setBrandName(dto.getBrandName());
        // publishStatus: 1 = 上架
        indexDO.setIsOnSale(dto.getPublishStatus() != null && Integer.valueOf(1).equals(dto.getPublishStatus()));
        // tags: 逗号分隔字符串 -> 数组
        if (dto.getTags() != null && !dto.getTags().isBlank()) {
            indexDO.setTags(dto.getTags().split(","));
        }
        indexDO.setSpuSpecs(dto.getSpuSpecs());
        indexDO.setCreateTime(dto.getCreateTime());
        // suggest: completion suggester 输入
        indexDO.setSuggest(buildSuggest(dto.getSpuName(), dto.getSubTitle()));
        return indexDO;
    }

    /**
     * 构建搜索建议 Completion 对象
     *
     * @param spuName  商品名称
     * @param subTitle 副标题
     * @return Completion 对象，无有效输入时返回空 Completion
     */
    private static Completion buildSuggest(String spuName, String subTitle) {
        boolean hasName = spuName != null && !spuName.isBlank();
        boolean hasSub = subTitle != null && !subTitle.isBlank();
        if (!hasName && !hasSub) {
            return new Completion(new String[0]);
        }
        if (hasName && hasSub) {
            return new Completion(new String[]{spuName, subTitle});
        }
        if (hasName) {
            return new Completion(new String[]{spuName});
        }
        return new Completion(new String[]{subTitle});
    }
}

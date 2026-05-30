package com.mall.product.VO;

import lombok.Data;

@Data
public class BrandVO {
    /** 品牌 ID */
    private String brandId;
    /** 品牌名称 */
    private String name;
    /** Logo URL */
    private String logo;
    /** 品牌简介 */
    private String description;
    /** 排序值 */
    private Integer sortOrder;
}

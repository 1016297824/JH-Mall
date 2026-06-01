package com.mall.product.VO;

import lombok.Data;

/**
 * 品牌 VO
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
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

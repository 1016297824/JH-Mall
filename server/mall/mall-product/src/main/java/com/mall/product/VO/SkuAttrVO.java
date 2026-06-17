package com.mall.product.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SKU 销售属性键值对
 *
 * <p>对应 DB attrs_json 中的 {"k":"颜色","v":"蓝色"}</p>
 *
 * @author JH-Mall
 * @date 2026/06/17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkuAttrVO {
    /** 属性名（如"颜色"） */
    private String k;
    /** 属性值（如"蓝色"） */
    private String v;
}

package com.mall.product.VO;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryVO {
    /** 类目 ID */
    private String categoryId;
    /** 父类目 ID，0=顶级 */
    private String parentId;
    /** 类目名称 */
    private String name;
    /** 层级：1/2/3 */
    private Integer level;
    /** 图标 URL */
    private String icon;
    /** 排序值 */
    private Integer sortOrder;
    /** 路径，如 /1/2/3 */
    private String path;
    /** 子类目 */
    private List<CategoryVO> children = new ArrayList<>();
}

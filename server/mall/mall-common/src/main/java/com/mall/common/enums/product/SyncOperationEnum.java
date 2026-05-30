package com.mall.common.enums.product;

import lombok.Getter;

/**
 * 搜索索引同步操作枚举
 *
 * @author JH-Mall
 * @date 2026/05/30
 */
@Getter
public enum SyncOperationEnum {

    /** 新增或更新索引 */
    UPSERT("UPSERT", "新增或更新"),
    /** 删除索引 */
    DELETE("DELETE", "删除");

    /** 操作码 */
    private final String code;
    /** 操作描述 */
    private final String desc;

    SyncOperationEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

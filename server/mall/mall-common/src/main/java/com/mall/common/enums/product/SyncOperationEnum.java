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
    UPSERT("UPSERT", "新增或更新"),
    DELETE("DELETE", "删除");

    private final String code;
    private final String desc;

    SyncOperationEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

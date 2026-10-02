package com.mall.order.dto.request;

import lombok.Data;

/**
 * 更新购物车请求
 *
 * <p>用于「修改数量」与「修改选中状态」两个端点，
 * 购物车项 ID 由路径参数 {@code /items/{id}} 携带。</p>
 *
 * <p>两个字段均可为 {@code null}：为 null 表示该项不修改。
 * 两者都为 null 时调用方应报错，但本层不拦截。</p>
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Data
public class UpdateCartRequest {

    /** 新数量，必须大于 0；null 表示不修改数量 */
    private Integer quantity;

    /** 选中状态，1=选中 0=未选；null 表示不修改选中状态 */
    private Integer isSelected;
}
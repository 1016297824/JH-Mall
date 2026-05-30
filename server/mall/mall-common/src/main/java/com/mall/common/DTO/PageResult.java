package com.mall.common.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果 DTO
 *
 * @author JH-Mall
 * @date 2026/05/30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    /** 当前页码 */
    private int page;
    /** 每页条数 */
    private int size;
    /** 总条数 */
    private long total;
    /** 数据列表 */
    private List<T> rows;

    public static <T> PageResult<T> of(int page, int size, long total, List<T> rows) {
        return new PageResult<>(page, size, total, rows);
    }
}

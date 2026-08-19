package com.ruoyi.twin.common.result;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.github.pagehelper.PageInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页结果包装，作为 R 的 data 使用
 *
 * @param <T> 记录类型
 * @author lvfan
 */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当页记录
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页码，从 1 开始
     */
    private long current;

    /**
     * 每页条数
     */
    private long size;

    /**
     * 由 PageHelper 分页对象转换
     */
    public static <T> PageResult<T> of(PageInfo<T> page) {
        return new PageResult<>(page.getList(), page.getTotal(), page.getPageNum(), page.getPageSize());
    }

    /**
     * 由已有集合手工组装，用于转换分页记录类型
     */
    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
        return new PageResult<>(records == null ? Collections.emptyList() : records, total, current, size);
    }
}

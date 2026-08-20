package com.ruoyi.twin.common.util;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.pagehelper.Page;

/**
 * PageHelper 分页结果转换工具。
 */
public final class PageMappingUtils {

    private PageMappingUtils() {
    }

    /**
     * 转换分页记录，并保留 PageHelper 的总数与页码元数据。
     *
     * @param source    原始查询结果
     * @param converter 记录转换函数
     * @param <S>       原始类型
     * @param <T>       目标类型
     * @return 转换后的列表；原列表为 Page 时返回带相同分页元数据的 Page
     */
    public static <S, T> List<T> map(List<S> source, Function<S, T> converter) {
        List<T> records = source.stream().map(converter).collect(Collectors.toList());
        if (!(source instanceof Page<?> sourcePage)) {
            return records;
        }

        Page<T> targetPage = new Page<>(sourcePage.getPageNum(), sourcePage.getPageSize(), sourcePage.isCount());
        targetPage.setTotal(sourcePage.getTotal());
        targetPage.setPages(sourcePage.getPages());
        targetPage.addAll(records);
        return targetPage;
    }
}

package com.zfh.iot.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zfh.iot.common.result.PageResult;

public class PageUtils {

    public static <T> Page<T> getPage(long page, long size) {
        return new Page<>(page, size);
    }

    public static <T> PageResult<T> convert(IPage<T> page) {
        return PageResult.of(page.getRecords(), page.getTotal(), 
                            page.getCurrent(), page.getSize());
    }
}

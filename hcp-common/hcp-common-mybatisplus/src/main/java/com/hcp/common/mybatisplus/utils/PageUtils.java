package com.hcp.common.mybatisplus.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hcp.common.core.text.Convert;
import com.hcp.common.core.utils.ServletUtils;
import com.hcp.common.mybatisplus.constant.MybatisPageConstants;

/**
 * 分页参数工具
 *
 * <p>统一从请求参数中解析 pageNum / pageSize，消除各 Service 中重复手写的 Page 构造代码。</p>
 *
 * @author hcp
 */
public class PageUtils
{
    /** 默认当前页 */
    private static final long DEFAULT_PAGE_NUM = 1L;

    /** 默认每页条数 */
    private static final long DEFAULT_PAGE_SIZE = 10L;

    private PageUtils()
    {
    }

    /**
     * 按请求参数构建分页对象，缺失时使用默认值（第 1 页，每页 10 条）
     *
     * @return MyBatis Plus 分页对象
     */
    public static <T> Page<T> buildPage()
    {
        return new Page<>(getPageNum(), getPageSize());
    }

    /**
     * 构建指定页码与条数的分页对象
     */
    public static <T> Page<T> buildPage(long pageNum, long pageSize)
    {
        return new Page<>(pageNum, pageSize);
    }

    /**
     * 取当前页码
     */
    public static long getPageNum()
    {
        return Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_NUM), DEFAULT_PAGE_NUM);
    }

    /**
     * 取每页条数
     */
    public static long getPageSize()
    {
        return Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_SIZE), DEFAULT_PAGE_SIZE);
    }
}

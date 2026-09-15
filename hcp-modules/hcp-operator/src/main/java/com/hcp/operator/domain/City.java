package com.hcp.operator.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hcp.common.core.annotation.Excel;
import lombok.Data;

import java.io.Serializable;

/**
 * 省市管理对象 c_city(公共表,仅有 Id/Name/Pid 三列,无 tenant_id/create_time 等字段)
 *
 * @author hcp
 * @date 2024-08-06
 */
@Data
@TableName("c_city")
public class City implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 城市代码 */
    @TableId(value = "Id")
    @Excel(name = "城市代码")
    private Long Id;

    /** 城市名称 */
    @Excel(name = "城市名称")
    private String Name;

    /** 省级代码 */
    @Excel(name = "省级代码")
    private Long Pid;

}

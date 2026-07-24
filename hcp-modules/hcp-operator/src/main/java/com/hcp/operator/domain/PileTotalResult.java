package com.hcp.operator.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hcp.common.core.annotation.Excel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PileTotalResult implements Serializable {

    @Schema(description = "端口状态")
    private String portStatus;

    @Excel(name = "充电桩编号")
    @Schema(description = "充电桩编号")
    private String chargeNo;

    //    @Excel(name = "站点编号",order = 1)
    @Schema(description = "站点编号")
    private String plotId;

    @Excel(name = "站点名称")
    @Schema(description = "站点名称")
    private String plotName;

    @Excel(name = "充电桩名称")
    @Schema(description = "充电桩名称")
    private String chargeName;

    //    @Excel(name = "代理商id")
    @Schema(description = "代理商id")
    private String userId;

    @Excel(name = "运行状态")
    @Schema(description = "运行状态 ")
    private String runningStatus;

    @Excel(name = "用电量")
    @Schema(description = "用电量")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalPowerConsumption;

    @Excel(name = "充电时长")
    @Schema(description = "充电时长")
    private String chargeTotalHour;

    @Excel(name = "服务费")
    @Schema(description = "服务费")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal serviceFee;

    @Excel(name = "充电费用")
    @Schema(description = "充电费用")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal chargeFee;

    @Excel(name = "总费用")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(description = "总费用")
    private BigDecimal totalAmount;

    @Excel(name = "充电次数")
    @Schema(description = "充电次数")
    private Integer chargeTotalTimes;
}

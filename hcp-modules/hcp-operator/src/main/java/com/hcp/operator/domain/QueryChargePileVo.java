package com.hcp.operator.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class QueryChargePileVo  {

    private int pageNum;
    private int pageSize;

    @Schema(description = "用户")
    private Long userId;

    @Schema(description = "设备类型")
    private String deviceType;

    @Schema(description = "充电桩编号")
    private String chargeNo;

    @Schema(description = "充电桩类型/快充慢充")
    private String pileType;

    @Schema(description = "充电桩状态/启动禁用")
    private String pileStatus;

    @Schema(description = "运行状态")
    private String runningStatus;

    @Schema(description = "端口状态")
    private String portStatus;

    private String plotId;

    private String plotName;



    @Schema(description = "是否急停")
    private String stopStatus;

    private String startTime;

    private String endTime;

    private String keyWord;




}

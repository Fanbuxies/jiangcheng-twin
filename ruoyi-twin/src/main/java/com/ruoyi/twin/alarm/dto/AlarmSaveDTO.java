package com.ruoyi.twin.alarm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 告警管理页新增和编辑入参。
 */
@Data
public class AlarmSaveDTO {

    /** 监测对象主键 */
    @NotNull
    @Min(1)
    private Long deviceId;

    /** 监测对象类型 */
    @NotBlank
    @Size(max = 16)
    private String objectType;

    /** 告警类型 */
    @NotBlank
    @Size(max = 32)
    private String alarmType;

    /** 告警级别 */
    @NotNull
    @Min(0)
    @Max(2)
    private Integer alarmLevel;

    /** 告警指标 JSON 文本 */
    @Size(max = 4000)
    private String alarmValueJson;

    /** 处理状态 */
    @NotBlank
    @Size(max = 16)
    private String status;
}

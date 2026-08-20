package com.ruoyi.twin.alarm.dto;

import lombok.Data;

/**
 * 告警管理页筛选参数。
 */
@Data
public class AlarmPageQuery {

    /** 监测对象主键 */
    private Long deviceId;

    /** 监测对象类型 */
    private String objectType;

    /** 告警类型 */
    private String alarmType;

    /** 告警级别 */
    private Integer alarmLevel;

    /** 处理状态 */
    private String status;
}

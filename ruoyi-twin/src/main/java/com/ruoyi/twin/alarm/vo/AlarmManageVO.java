package com.ruoyi.twin.alarm.vo;

import java.io.Serializable;
import java.time.OffsetDateTime;

import lombok.Data;

/**
 * 告警管理页视图对象。
 */
@Data
public class AlarmManageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 监测对象主键 */
    private Long deviceId;

    /** 监测对象类型 */
    private String objectType;

    /** 告警类型 */
    private String alarmType;

    /** 告警级别 */
    private Integer alarmLevel;

    /** 告警指标 JSON 文本 */
    private String alarmValueJson;

    /** 处理状态 */
    private String status;

    /** 发生时间 */
    private OffsetDateTime occurTime;

    /** 关闭时间 */
    private OffsetDateTime closeTime;

    /** 入库时间 */
    private OffsetDateTime createdAt;
}

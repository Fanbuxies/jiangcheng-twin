package com.ruoyi.twin.alarm.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;

import lombok.Data;

/**
 * 告警数据对象，对应 t_alarm
 *
 * <p>alarm_value 为 jsonb 列，Java 侧以文本收发。</p>
 *
 * @author lvfan
 */
@Data
public class AlarmDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 设备主键
     */
    private Long deviceId;

    /**
     * 告警类型：SMOKE_ALARM / WATER_LEAK / TEMP_HIGH / CURRENT_HIGH / STREAM_LOST
     */
    private String alarmType;

    /**
     * 告警级别：0 正常 1 预警 2 告警
     */
    private Integer alarmLevel;

    /**
     * 触发时的指标快照 JSON 文本
     */
    private String alarmValueJson;

    /**
     * 处理状态：PENDING / CONFIRMED / CLOSED
     */
    private String status;

    /**
     * 发生时间
     */
    private OffsetDateTime occurTime;

    /**
     * 关闭时间
     */
    private OffsetDateTime closeTime;
}

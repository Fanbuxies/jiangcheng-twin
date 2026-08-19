package com.ruoyi.twin.simulator.config;

import tools.jackson.databind.ObjectMapper;
import com.ruoyi.twin.alarm.service.AlarmService;
import com.ruoyi.twin.common.config.AppProperties;
import com.ruoyi.twin.device.mapper.DeviceMapper;
import com.ruoyi.twin.device.service.DeviceRealtimeService;
import com.ruoyi.twin.facility.mapper.FacilityMapper;
import com.ruoyi.twin.simulator.DeviceSimulateTask;
import com.ruoyi.twin.simulator.FacilitySimulateTask;
import com.ruoyi.twin.ws.RealtimeWebSocketHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 模拟器装配。开关关闭时连调度线程都不创建
 *
 * @author lvfan
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.simulator", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class SimulatorConfig {

    @Bean
    public DeviceSimulateTask deviceSimulateTask(DeviceMapper deviceMapper,
                                                 DeviceRealtimeService deviceRealtimeService,
                                                 AlarmService alarmService,
                                                 RealtimeWebSocketHandler realtimeWebSocketHandler,
                                                 AppProperties appProperties,
                                                 ObjectMapper objectMapper) {
        return new DeviceSimulateTask(deviceMapper, deviceRealtimeService, alarmService,
                realtimeWebSocketHandler, appProperties, objectMapper);
    }

    @Bean
    public FacilitySimulateTask facilitySimulateTask(FacilityMapper facilityMapper,
                                                    DeviceRealtimeService deviceRealtimeService,
                                                    AlarmService alarmService,
                                                    RealtimeWebSocketHandler realtimeWebSocketHandler,
                                                    AppProperties appProperties,
                                                    ObjectMapper objectMapper) {
        return new FacilitySimulateTask(facilityMapper, deviceRealtimeService, alarmService,
                realtimeWebSocketHandler, appProperties, objectMapper);
    }
}

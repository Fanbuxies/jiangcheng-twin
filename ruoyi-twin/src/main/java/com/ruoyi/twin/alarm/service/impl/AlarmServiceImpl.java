package com.ruoyi.twin.alarm.service.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.ruoyi.twin.alarm.dto.AlarmCreateDTO;
import com.ruoyi.twin.alarm.mapper.AlarmMapper;
import com.ruoyi.twin.alarm.service.AlarmService;
import com.ruoyi.twin.alarm.vo.AlarmVO;
import com.ruoyi.twin.common.enums.ObjectTypeEnum;
import com.ruoyi.twin.common.exception.BizException;
import com.ruoyi.twin.common.result.ResultCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 告警服务实现
 *
 * @author lvfan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements AlarmService {

    /**
     * 新建告警的初始状态
     */
    private static final String STATUS_PENDING = "PENDING";

    private final AlarmMapper alarmMapper;

    private final ObjectMapper objectMapper;

    @Override
    public void createAlarms(List<AlarmCreateDTO> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        alarmMapper.batchInsert(list);
    }

    @Override
    public Set<Long> listPendingDeviceIds(ObjectTypeEnum objectType) {
        List<Long> ids = alarmMapper.selectPendingDeviceIds(objectType.name());
        return CollectionUtils.isEmpty(ids) ? Collections.emptySet() : new HashSet<>(ids);
    }

    @Override
    public List<AlarmVO> toAlarmVoList(List<AlarmCreateDTO> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream().map(this::toVo).collect(Collectors.toList());
    }

    private AlarmVO toVo(AlarmCreateDTO dto) {
        AlarmVO vo = new AlarmVO();
        vo.setDeviceId(dto.getDeviceId());
        vo.setObjectType(dto.getObjectType());
        vo.setAlarmType(dto.getAlarmType());
        vo.setAlarmLevel(dto.getAlarmLevel());
        vo.setAlarmValue(parseJson(dto.getAlarmValueJson()));
        vo.setStatus(STATUS_PENDING);
        vo.setOccurTime(dto.getOccurTime());
        return vo;
    }

    private JsonNode parseJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JacksonException e) {
            log.error("告警指标 JSON 无法解析，长度 {}", json.length(), e);
            throw new BizException(ResultCodeEnum.SYSTEM_ERROR);
        }
    }
}

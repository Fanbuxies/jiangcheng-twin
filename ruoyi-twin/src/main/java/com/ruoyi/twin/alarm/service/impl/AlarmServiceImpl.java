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
import com.ruoyi.twin.alarm.dto.AlarmPageQuery;
import com.ruoyi.twin.alarm.dto.AlarmSaveDTO;
import com.ruoyi.twin.alarm.entity.AlarmDO;
import com.ruoyi.twin.alarm.enums.AlarmTypeEnum;
import com.ruoyi.twin.alarm.mapper.AlarmMapper;
import com.ruoyi.twin.alarm.service.AlarmService;
import com.ruoyi.twin.alarm.vo.AlarmManageVO;
import com.ruoyi.twin.alarm.vo.AlarmVO;
import com.ruoyi.twin.common.enums.ObjectTypeEnum;
import com.ruoyi.twin.common.exception.BizException;
import com.ruoyi.twin.common.result.ResultCodeEnum;
import com.ruoyi.twin.common.util.PageMappingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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

    /** 已确认状态 */
    private static final String STATUS_CONFIRMED = "CONFIRMED";

    /** 已关闭状态 */
    private static final String STATUS_CLOSED = "CLOSED";

    private final AlarmMapper alarmMapper;

    private final ObjectMapper objectMapper;

    @Override
    public List<AlarmManageVO> listAlarms(AlarmPageQuery query) {
        List<AlarmDO> result = alarmMapper.selectAlarmList(query);
        return PageMappingUtils.map(result, AlarmServiceImpl::toManageVo);
    }

    @Override
    public AlarmManageVO getAlarm(Long id) {
        return toManageVo(requireAlarm(id));
    }

    @Override
    public Long createAlarm(AlarmSaveDTO dto) {
        AlarmDO alarm = toSaveDo(dto);
        alarmMapper.insertAlarm(alarm);
        return alarm.getId();
    }

    @Override
    public void updateAlarm(Long id, AlarmSaveDTO dto) {
        requireAlarm(id);
        AlarmDO alarm = toSaveDo(dto);
        alarm.setId(id);
        if (alarmMapper.updateAlarm(alarm) == 0) {
            throw new BizException(ResultCodeEnum.NOT_FOUND, "告警不存在：" + id);
        }
    }

    @Override
    public void deleteAlarm(Long id) {
        requireAlarm(id);
        alarmMapper.deleteById(id);
    }

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

    private AlarmDO requireAlarm(Long id) {
        AlarmDO alarm = alarmMapper.selectById(id);
        if (alarm == null) {
            throw new BizException(ResultCodeEnum.NOT_FOUND, "告警不存在：" + id);
        }
        return alarm;
    }

    private AlarmDO toSaveDo(AlarmSaveDTO dto) {
        String objectType = normalizeObjectType(dto.getObjectType());
        String alarmType = normalizeAlarmType(dto.getAlarmType());
        String status = normalizeStatus(dto.getStatus());
        validateJson(dto.getAlarmValueJson());

        AlarmDO alarm = new AlarmDO();
        BeanUtils.copyProperties(dto, alarm);
        alarm.setObjectType(objectType);
        alarm.setAlarmType(alarmType);
        alarm.setStatus(status);
        alarm.setAlarmValueJson(trimToNull(dto.getAlarmValueJson()));
        return alarm;
    }

    private static AlarmManageVO toManageVo(AlarmDO alarm) {
        AlarmManageVO vo = new AlarmManageVO();
        BeanUtils.copyProperties(alarm, vo);
        return vo;
    }

    private static String normalizeObjectType(String objectType) {
        String normalized = trimToNull(objectType);
        if (normalized == null || !isObjectType(normalized)) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "监测对象类型不支持，可选值：DEVICE/FACILITY");
        }
        return normalized;
    }

    private static boolean isObjectType(String objectType) {
        return java.util.Arrays.stream(ObjectTypeEnum.values())
                .anyMatch(type -> type.name().equals(objectType));
    }

    private static String normalizeAlarmType(String alarmType) {
        String normalized = trimToNull(alarmType);
        if (normalized == null || !isAlarmType(normalized)) {
            String supported = java.util.Arrays.stream(AlarmTypeEnum.values())
                    .map(Enum::name)
                    .collect(Collectors.joining("/"));
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "告警类型不支持，可选值：" + supported);
        }
        return normalized;
    }

    private static boolean isAlarmType(String alarmType) {
        return java.util.Arrays.stream(AlarmTypeEnum.values())
                .anyMatch(type -> type.name().equals(alarmType));
    }

    private static String normalizeStatus(String status) {
        String normalized = trimToNull(status);
        if (!STATUS_PENDING.equals(normalized)
                && !STATUS_CONFIRMED.equals(normalized)
                && !STATUS_CLOSED.equals(normalized)) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR,
                    "告警状态不支持，可选值：PENDING/CONFIRMED/CLOSED");
        }
        return normalized;
    }

    private void validateJson(String json) {
        if (!StringUtils.hasText(json)) {
            return;
        }
        try {
            objectMapper.readTree(json);
        } catch (JacksonException e) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "告警指标 JSON 格式不正确");
        }
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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

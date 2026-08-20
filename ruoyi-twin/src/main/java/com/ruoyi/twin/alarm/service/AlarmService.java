package com.ruoyi.twin.alarm.service;

import java.util.List;
import java.util.Set;

import com.ruoyi.twin.alarm.dto.AlarmCreateDTO;
import com.ruoyi.twin.alarm.dto.AlarmPageQuery;
import com.ruoyi.twin.alarm.dto.AlarmSaveDTO;
import com.ruoyi.twin.alarm.vo.AlarmManageVO;
import com.ruoyi.twin.alarm.vo.AlarmVO;
import com.ruoyi.twin.common.enums.ObjectTypeEnum;

/**
 * 告警服务
 *
 * @author lvfan
 */
public interface AlarmService {

    /**
     * 按条件查询告警列表
     *
     * @param query 筛选参数
     * @return 告警列表
     */
    List<AlarmManageVO> listAlarms(AlarmPageQuery query);

    /**
     * 按主键查询告警
     *
     * @param id 主键
     * @return 告警详情
     */
    AlarmManageVO getAlarm(Long id);

    /**
     * 新增告警
     *
     * @param dto 告警入参
     * @return 新告警主键
     */
    Long createAlarm(AlarmSaveDTO dto);

    /**
     * 更新告警
     *
     * @param id  主键
     * @param dto 告警入参
     */
    void updateAlarm(Long id, AlarmSaveDTO dto);

    /**
     * 删除告警
     *
     * @param id 主键
     */
    void deleteAlarm(Long id);

    /**
     * 批量新增告警，状态固定为 PENDING
     *
     * @param list 告警入参，空集合直接返回
     */
    void createAlarms(List<AlarmCreateDTO> list);

    /**
     * 查询存在待处理告警的监测对象主键集合，供模拟器去重
     *
     * @param objectType 监测对象类型，设备与设施共用同一张告警表，须显式区分
     * @return 无数据返回空集合
     */
    Set<Long> listPendingDeviceIds(ObjectTypeEnum objectType);

    /**
     * 把新增入参转成推送体
     *
     * @param list 告警入参
     * @return 推送体列表，入参为空时返回空集合
     */
    List<AlarmVO> toAlarmVoList(List<AlarmCreateDTO> list);
}

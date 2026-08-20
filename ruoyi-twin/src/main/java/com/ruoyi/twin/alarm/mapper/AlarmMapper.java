package com.ruoyi.twin.alarm.mapper;

import java.util.List;

import com.ruoyi.twin.alarm.dto.AlarmCreateDTO;
import com.ruoyi.twin.alarm.dto.AlarmPageQuery;
import com.ruoyi.twin.alarm.entity.AlarmDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 告警 mapper
 *
 * @author lvfan
 */
@Mapper
public interface AlarmMapper {

    /**
     * 按条件查询告警列表
     *
     * @param query 筛选参数
     * @return 告警列表
     */
    List<AlarmDO> selectAlarmList(AlarmPageQuery query);

    /**
     * 按主键查询告警
     *
     * @param id 主键
     * @return 查无返回 null
     */
    AlarmDO selectById(@Param("id") Long id);

    /**
     * 批量新增告警，状态固定为 PENDING
     *
     * @param list 告警入参，不可为空集合
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<AlarmCreateDTO> list);

    /**
     * 新增告警
     *
     * @param alarm 告警数据
     * @return 影响行数
     */
    int insertAlarm(@Param("alarm") AlarmDO alarm);

    /**
     * 更新告警可编辑字段
     *
     * @param alarm 告警数据
     * @return 影响行数
     */
    int updateAlarm(@Param("alarm") AlarmDO alarm);

    /**
     * 查询存在待处理告警的监测对象主键，供模拟器去重，避免同一对象反复刷告警
     *
     * @param objectType 监测对象类型：DEVICE / FACILITY
     * @return 监测对象主键列表，无数据返回空集合
     */
    List<Long> selectPendingDeviceIds(@Param("objectType") String objectType);

    /**
     * 按监测对象删除告警记录，删除设备台账前清理关联数据
     *
     * @param deviceId   监测对象主键
     * @param objectType 监测对象类型：DEVICE / FACILITY
     * @return 影响行数
     */
    int deleteByDeviceId(@Param("deviceId") Long deviceId,
                         @Param("objectType") String objectType);

    /**
     * 按主键删除告警
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}

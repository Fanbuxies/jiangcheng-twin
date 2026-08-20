package com.ruoyi.twin.web;

import java.util.List;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.twin.alarm.dto.AlarmPageQuery;
import com.ruoyi.twin.alarm.dto.AlarmSaveDTO;
import com.ruoyi.twin.alarm.service.AlarmService;
import com.ruoyi.twin.alarm.vo.AlarmManageVO;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 告警管理页面。
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/twin/alarm")
public class AlarmManageController extends BaseController {

    private static final String PREFIX = "twin/alarm";

    private final AlarmService alarmService;

    @RequiresPermissions("twin:alarm:view")
    @GetMapping()
    public String alarm() {
        return PREFIX + "/alarm";
    }

    @RequiresPermissions("twin:alarm:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(AlarmPageQuery query) {
        startPage();
        List<AlarmManageVO> list = alarmService.listAlarms(query);
        return getDataTable(list);
    }

    @RequiresPermissions("twin:alarm:add")
    @GetMapping("/add")
    public String add() {
        return PREFIX + "/add";
    }

    @RequiresPermissions("twin:alarm:add")
    @Log(title = "告警管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@Validated AlarmSaveDTO dto) {
        return toAjax(alarmService.createAlarm(dto) != null);
    }

    @RequiresPermissions("twin:alarm:edit")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap modelMap) {
        modelMap.put("alarm", alarmService.getAlarm(id));
        return PREFIX + "/edit";
    }

    @RequiresPermissions("twin:alarm:edit")
    @Log(title = "告警管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(@RequestParam("id") Long id, @Validated AlarmSaveDTO dto) {
        alarmService.updateAlarm(id, dto);
        return toAjax(true);
    }

    @RequiresPermissions("twin:alarm:remove")
    @Log(title = "告警管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam("ids") Long id) {
        alarmService.deleteAlarm(id);
        return toAjax(true);
    }
}

package com.ruoyi.twin.web;

import java.util.List;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.twin.device.dto.DevicePageQuery;
import com.ruoyi.twin.device.dto.DeviceSaveDTO;
import com.ruoyi.twin.device.service.DeviceService;
import com.ruoyi.twin.device.vo.DevicePageVO;
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
 * 设备管理页面。
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/twin/device")
public class DeviceManageController extends BaseController {

    private static final String PREFIX = "twin/device";

    private final DeviceService deviceService;

    @RequiresPermissions("twin:device:view")
    @GetMapping()
    public String device() {
        return PREFIX + "/device";
    }

    @RequiresPermissions("twin:device:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(DevicePageQuery query) {
        startPage();
        List<DevicePageVO> list = deviceService.listDevices(query);
        return getDataTable(list);
    }

    @RequiresPermissions("twin:device:add")
    @GetMapping("/add")
    public String add() {
        return PREFIX + "/add";
    }

    @RequiresPermissions("twin:device:add")
    @Log(title = "设备管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@Validated DeviceSaveDTO dto) {
        return toAjax(deviceService.createDevice(dto) != null);
    }

    @RequiresPermissions("twin:device:edit")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap modelMap) {
        modelMap.put("device", deviceService.getDevice(id));
        return PREFIX + "/edit";
    }

    @RequiresPermissions("twin:device:edit")
    @Log(title = "设备管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(@RequestParam("id") Long id, @Validated DeviceSaveDTO dto) {
        deviceService.updateDevice(id, dto);
        return toAjax(true);
    }

    @RequiresPermissions("twin:device:remove")
    @Log(title = "设备管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam("ids") Long id) {
        deviceService.deleteDevice(id);
        return toAjax(true);
    }
}

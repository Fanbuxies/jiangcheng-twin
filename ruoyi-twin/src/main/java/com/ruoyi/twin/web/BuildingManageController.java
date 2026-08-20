package com.ruoyi.twin.web;

import java.util.List;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.twin.building.dto.BuildingPageQuery;
import com.ruoyi.twin.building.dto.BuildingSaveDTO;
import com.ruoyi.twin.building.service.BuildingService;
import com.ruoyi.twin.building.vo.BuildingPageVO;
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
 * 建筑管理页面。
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/twin/building")
public class BuildingManageController extends BaseController {

    private static final String PREFIX = "twin/building";

    private final BuildingService buildingService;

    @RequiresPermissions("twin:building:view")
    @GetMapping()
    public String building() {
        return PREFIX + "/building";
    }

    @RequiresPermissions("twin:building:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(BuildingPageQuery query) {
        startPage();
        List<BuildingPageVO> list = buildingService.listBuildings(query);
        return getDataTable(list);
    }

    @RequiresPermissions("twin:building:add")
    @GetMapping("/add")
    public String add() {
        return PREFIX + "/add";
    }

    @RequiresPermissions("twin:building:add")
    @Log(title = "建筑管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@Validated BuildingSaveDTO dto) {
        return toAjax(buildingService.createBuilding(dto) != null);
    }

    @RequiresPermissions("twin:building:edit")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap modelMap) {
        modelMap.put("building", buildingService.getDetail(id));
        return PREFIX + "/edit";
    }

    @RequiresPermissions("twin:building:edit")
    @Log(title = "建筑管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(@RequestParam("id") Long id, @Validated BuildingSaveDTO dto) {
        buildingService.updateBuilding(id, dto);
        return toAjax(true);
    }

    @RequiresPermissions("twin:building:remove")
    @Log(title = "建筑管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam("ids") Long id) {
        buildingService.deleteBuilding(id);
        return toAjax(true);
    }
}

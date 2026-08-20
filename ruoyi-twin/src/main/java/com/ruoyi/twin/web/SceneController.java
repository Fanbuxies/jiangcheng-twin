package com.ruoyi.twin.web;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 三维底座页面入口。
 */
@Controller
@RequestMapping("/twin")
public class SceneController {

    /**
     * 打开三维底座。
     *
     * @return 三维场景模板
     */
    @RequiresPermissions("twin:scene:view")
    @GetMapping("/index")
    public String index() {
        return "twin/index";
    }
}

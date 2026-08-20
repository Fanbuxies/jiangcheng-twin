package com.ruoyi.twin.gate;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.twin.common.util.PageMappingUtils;
import com.ruoyi.twin.web.AlarmManageController;
import com.ruoyi.twin.web.BuildingManageController;
import com.ruoyi.twin.web.DeviceManageController;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 阶段 5：验证三套管理页 Controller 接入若依体系。
 */
public final class ManagementControllerGate {

    private ManagementControllerGate() {
    }

    public static void main(String[] args) throws Exception {
        requireController(BuildingManageController.class, "/twin/building", "twin:building");
        requireController(DeviceManageController.class, "/twin/device", "twin:device");
        requireController(AlarmManageController.class, "/twin/alarm", "twin:alarm");
        requireTemplates();
        requirePageMetadata();
        System.out.println("MANAGEMENT_CONTROLLER_PASS controllers=3 routes=21 templates=9 total=5 rows=2");
    }

    private static void requireController(Class<?> controllerClass, String expectedPath, String permissionPrefix)
            throws IOException {
        require(BaseController.class.isAssignableFrom(controllerClass),
                controllerClass.getSimpleName() + " 未继承 BaseController");
        require(controllerClass.isAnnotationPresent(Controller.class),
                controllerClass.getSimpleName() + " 缺少 Controller 注解");
        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
        require(requestMapping != null && requestMapping.value().length == 1
                        && expectedPath.equals(requestMapping.value()[0]),
                controllerClass.getSimpleName() + " 路径不正确");

        for (Field field : controllerClass.getDeclaredFields()) {
            require(!field.getType().getSimpleName().endsWith("Mapper"),
                    controllerClass.getSimpleName() + " 不得注入 Mapper：" + field.getName());
        }

        requireRoute(controllerClass, GetMapping.class, "", permissionPrefix + ":view", String.class, null);
        requireRoute(controllerClass, PostMapping.class, "/list", permissionPrefix + ":list",
                TableDataInfo.class, null);
        requireRoute(controllerClass, GetMapping.class, "/add", permissionPrefix + ":add", String.class, null);
        requireRoute(controllerClass, PostMapping.class, "/add", permissionPrefix + ":add",
                AjaxResult.class, BusinessType.INSERT);
        requireRoute(controllerClass, GetMapping.class, "/edit/{id}", permissionPrefix + ":edit",
                String.class, null);
        requireRoute(controllerClass, PostMapping.class, "/edit", permissionPrefix + ":edit",
                AjaxResult.class, BusinessType.UPDATE);
        requireRoute(controllerClass, PostMapping.class, "/remove", permissionPrefix + ":remove",
                AjaxResult.class, BusinessType.DELETE);

        String source = read(Path.of("ruoyi-twin", "src", "main", "java", "com", "ruoyi", "twin", "web",
                controllerClass.getSimpleName() + ".java"));
        require(source.matches("(?s).*startPage\\(\\);\\s*List<[^>]+>\\s+list\\s*=\\s*\\w+Service\\.list\\w+\\(query\\);.*"),
                controllerClass.getSimpleName() + " 的 startPage 后未紧随列表查询");
    }

    private static void requireRoute(Class<?> controllerClass, Class<? extends Annotation> mappingType,
                                     String path, String permission, Class<?> returnType,
                                     BusinessType businessType) {
        Method method = Arrays.stream(controllerClass.getDeclaredMethods())
                .filter(item -> item.isAnnotationPresent(mappingType))
                .filter(item -> routePath(item, mappingType).equals(path))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        controllerClass.getSimpleName() + " 缺少路由 " + mappingType.getSimpleName() + " " + path));
        RequiresPermissions requiresPermissions = method.getAnnotation(RequiresPermissions.class);
        require(requiresPermissions != null && Arrays.asList(requiresPermissions.value()).contains(permission),
                controllerClass.getSimpleName() + "." + method.getName() + " 权限不正确");
        require(returnType.equals(method.getReturnType()),
                controllerClass.getSimpleName() + "." + method.getName() + " 返回类型不正确");
        if (mappingType == PostMapping.class) {
            require(method.isAnnotationPresent(ResponseBody.class),
                    controllerClass.getSimpleName() + "." + method.getName() + " 缺少 ResponseBody");
        }
        if (businessType != null) {
            Log log = method.getAnnotation(Log.class);
            require(log != null && log.businessType() == businessType,
                    controllerClass.getSimpleName() + "." + method.getName() + " 操作日志类型不正确");
        }
    }

    private static String routePath(Method method, Class<? extends Annotation> mappingType) {
        if (mappingType == GetMapping.class) {
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            return mapping == null || mapping.value().length == 0 ? "" : mapping.value()[0];
        }
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        return mapping == null || mapping.value().length == 0 ? "" : mapping.value()[0];
    }

    private static void requireTemplates() throws IOException {
        Path templateRoot = Path.of("ruoyi-admin", "src", "main", "resources", "templates", "twin");
        for (String module : List.of("building", "device", "alarm")) {
            for (String name : List.of(module + ".html", "add.html", "edit.html")) {
                Path template = templateRoot.resolve(module).resolve(name);
                require(Files.isRegularFile(template), "缺少模板：" + template);
                String content = read(template);
                require(content.contains("include ::"), "模板未复用若依公共片段：" + template);
                require(!content.contains("removeAll"), "单条删除页面不得展示批量删除：" + template);
                if (name.equals(module + ".html")) {
                    require(content.contains("escape: true"), "列表未开启文本转义：" + template);
                }
            }
        }

        for (String name : List.of("add.html", "edit.html")) {
            String content = read(templateRoot.resolve("building").resolve(name));
            require(!content.contains("name=\"footprint\"") && !content.contains("name=\"center\""),
                    "建筑表单不得提交几何字段：" + name);
            require(content.contains("type=\"number\" name=\"lon\"")
                            && content.contains("type=\"number\" name=\"lat\""),
                    "建筑表单必须使用经纬度数字输入：" + name);
        }

        requireServiceUsesPageMapping("building", "BuildingServiceImpl.java");
        requireServiceUsesPageMapping("device", "DeviceServiceImpl.java");
        requireServiceUsesPageMapping("alarm", "AlarmServiceImpl.java");
    }

    private static void requireServiceUsesPageMapping(String module, String fileName) throws IOException {
        Path source = Path.of("ruoyi-twin", "src", "main", "java", "com", "ruoyi", "twin", module,
                "service", "impl", fileName);
        require(read(source).contains("PageMappingUtils.map"), "管理列表转换丢失 PageHelper 元数据：" + fileName);
    }

    private static void requirePageMetadata() {
        Page<String> source = new Page<>(2, 2, true);
        source.setTotal(5);
        source.add("first");
        source.add("second");

        List<Integer> result = PageMappingUtils.map(source, String::length);
        PageInfo<Integer> pageInfo = new PageInfo<>(result);
        require(result.size() == 2, "分页记录转换后条数不正确");
        require(pageInfo.getTotal() == 5, "分页记录转换后 total 丢失：" + pageInfo.getTotal());
        require(pageInfo.getTotal() > result.size(), "回归样例必须覆盖 total 大于当前页条数");
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}

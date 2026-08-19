package com.ruoyi.twin.gate;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Set;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.twin.common.exception.GlobalExceptionHandler;
import com.ruoyi.twin.common.result.R;
import com.ruoyi.twin.common.result.ResultCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 阶段 4：验证 twin 与宿主异常响应体系相互隔离。
 */
public final class ExceptionIsolationGate {

    private static final Set<String> EXPECTED_PACKAGES = Set.of(
            "com.ruoyi.twin.building",
            "com.ruoyi.twin.device",
            "com.ruoyi.twin.facility",
            "com.ruoyi.twin.alarm",
            "com.ruoyi.twin.stat");

    private ExceptionIsolationGate() {
    }

    public static void main(String[] args) {
        Class<GlobalExceptionHandler> handlerClass = GlobalExceptionHandler.class;
        RestControllerAdvice advice = handlerClass.getAnnotation(RestControllerAdvice.class);
        require(advice != null, "缺少 RestControllerAdvice 注解");
        require("twinGlobalExceptionHandler".equals(advice.name()), "twin advice Bean 名不正确");

        Set<String> actualPackages = Set.copyOf(Arrays.asList(advice.basePackages()));
        require(EXPECTED_PACKAGES.equals(actualPackages), "twin advice 包范围不正确：" + actualPackages);
        require(!actualPackages.contains("com.ruoyi.twin.web"), "管理页包不应由 twin advice 捕获");

        Order order = handlerClass.getAnnotation(Order.class);
        require(order != null && order.value() == 1, "twin advice 优先级不正确");

        HttpServletRequest request = request("POST", "/api/building/1");
        HttpRequestMethodNotSupportedException exception =
                new HttpRequestMethodNotSupportedException("POST", Set.of("GET"));
        R<Void> twinResult = new GlobalExceptionHandler().handleMethodNotSupported(exception, request);
        AjaxResult hostResult = AjaxResult.error(exception.getMessage());

        require(Integer.valueOf(ResultCodeEnum.METHOD_NOT_ALLOWED.getCode()).equals(twinResult.getCode()),
                "twin 异常码不正确：" + twinResult.getCode());
        require(Integer.valueOf(AjaxResult.Type.ERROR.value()).equals(hostResult.get(AjaxResult.CODE_TAG)),
                "宿主异常码不正确：" + hostResult.get(AjaxResult.CODE_TAG));

        System.out.printf("EXCEPTION_ISOLATION_PASS twinCode=%d hostCode=%s packages=%d order=%d%n",
                twinResult.getCode(), hostResult.get(AjaxResult.CODE_TAG), actualPackages.size(), order.value());
    }

    private static HttpServletRequest request(String method, String requestUri) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                ExceptionIsolationGate.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (proxy, invokedMethod, args) -> switch (invokedMethod.getName()) {
                    case "getMethod" -> method;
                    case "getRequestURI" -> requestUri;
                    default -> null;
                });
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}

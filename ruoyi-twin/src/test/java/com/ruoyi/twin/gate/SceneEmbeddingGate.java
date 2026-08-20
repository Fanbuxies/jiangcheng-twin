package com.ruoyi.twin.gate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 三维场景嵌入静态契约验证。
 */
public final class SceneEmbeddingGate {

    private SceneEmbeddingGate() {
    }

    public static void main(String[] args) throws Exception {
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path adminResources = projectRoot.resolve("ruoyi-admin/src/main/resources");
        Path sceneTemplate = adminResources.resolve("templates/twin/index.html");
        Path staticRoot = adminResources.resolve("static/twin");
        Path resourcesConfig = projectRoot.resolve(
                "ruoyi-framework/src/main/java/com/ruoyi/framework/config/ResourcesConfig.java");

        require(Files.isRegularFile(sceneTemplate), "缺少三维场景壳页");
        String template = Files.readString(sceneTemplate, StandardCharsets.UTF_8);
        require(template.contains("/twin/assets/"), "壳页资源未使用 /twin/ 子路径");
        require(Files.isDirectory(staticRoot.resolve("assets")), "缺少 Vite assets 产物");
        require(Files.isDirectory(staticRoot.resolve("cesium")), "缺少 Cesium 产物");
        require(!Files.exists(staticRoot.resolve("tiles")), "tiles 禁止复制进静态资源");

        String config = Files.readString(resourcesConfig, StandardCharsets.UTF_8);
        require(config.contains("/twin/tiles/**"), "缺少磁盘 tiles 资源映射");
        System.out.println("SCENE_EMBEDDING_PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}

/*
 * Copyright 2011-2035 詹波 (aifei.cn)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.aifei.util;

import cn.aifei.config.AifeiConfig;
import cn.aifei.core.Aifei;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * PathUtil
 */
public class PathUtil {

    static Path appHome;
    static Path webRootPath;
    static Path uploadPath;
    static Path downloadPath;
    static List<Path> classPathList;
    static boolean deployMode;
    static boolean isInitialized = false;

    /**
     * 指定 appHome
     */
    public static void setAppHome(Path appHome) {
        Objects.requireNonNull(appHome, "appHome can not be null.");
        PathUtil.appHome = appHome.toAbsolutePath().normalize();
    }

    /**
     * 指定 webRootPath
     */
    public static void setWebRootPath(Path webRootPath) {
        Objects.requireNonNull(webRootPath, "webRootPath can not be null.");
        PathUtil.webRootPath = webRootPath;
    }

    /**
     * 初始化整个 PathUtil。isInitialized 确保只被初始化一次
     */
    public synchronized static void init(AifeiConfig<?, ?> aifeiConfig) {
        if (!isInitialized) {
            resolveAppHome(aifeiConfig);
            resolveDeployMode();

            webRootPath = resolveWebRootPath().toAbsolutePath().normalize();
            isInitialized = true;
        }
    }

    /**
     * 解析 appHome，优先使用用户指定的值
     */
    private static void resolveAppHome(AifeiConfig<?, ?> aifeiConfig) {
        // 若用户指定 appHome，则不执行解析操作
        if (appHome == null) {
            AppHome appHomeObject = AppHome.of(aifeiConfig.getClass());
            appHome = appHomeObject.get();
            classPathList = appHomeObject.getClassPathList();
        }

        // 用户指定 appHome 未执行解析操作，但仍需通过 AppHome 获取 classPathList
        if (classPathList == null) {
            classPathList = AppHome.of(aifeiConfig.getClass()).getClassPathList();
        }
    }

    /**
     * 解析 deployMode
     *
     * <pre>
     * 部署模式逻辑，优先使用可靠性最高的特征：
     * 1: 若 appHome + "/lib" 目录存在，则认定为部署模式
     * 2: 若 appHome + "/config" 目录存在，则认定为部署模式
     * 3: 若 appHome + "/webapp" 目录存在，则认定为部署模式
     * 4: 若 class path 中存在 classes 结尾的目录，则认定为 "非部署" 模式
     * 5: 其它情况认定为部署模式，生产环境优先，开发环境在开发阶段可发现并处理
     * </pre>
     */
    private static void resolveDeployMode() {
        // 部署环境在 appHome 下可能存在 lib、config、webapp（fatjar 部署除外）
        Path appHome = PathUtil.getAppHome();
        String[] dirs = {"lib", "config", "webapp"};
        for (String dir : dirs) {
            Path cur = appHome.resolve(dir);
            if (Files.isDirectory(cur)) {
                deployMode = true;
                return;
            }
        }

        // 根据 class path 特征获取 deployMode
        List<Path> classPathList = PathUtil.getClassPathList();
        for (Path classPath : classPathList) {
            if (classPath.endsWith("classes")) {
                deployMode = false;
                return;
            }
        }

        deployMode = true;
    }

    /**
     * 解析 webRootPath
     */
    private static Path resolveWebRootPath() {
        // 优先使用用户指定 webRootPath
        if (webRootPath != null) {
            return appHome.resolve(webRootPath);
        }

        // 以 appHome 为起点，探测部署环境、开发环境的 webRootPath 目录
        String[] webappArray = {"webapp", "src/main/webapp", "WebRoot", "WebContent"};
        for (String dir : webappArray) {
            Path webapp = appHome.resolve(dir);
            if (Files.isDirectory(webapp)) {
                return webapp;
            }
        }

        // 兜底 appHome + "/webapp"
        return appHome.resolve("webapp");
    }

    /**
     * 解析 uploadPath
     */
    private synchronized static Path resolveUploadPath() {
        if (uploadPath == null) {
            Path up = Aifei.getSettings().getUploadPath();
            uploadPath = getWebRootPath().resolve(up).toAbsolutePath().normalize();
        }
        return uploadPath;
    }

    /**
     * 解析 downloadPath
     */
    private synchronized static Path resolveDownloadPath() {
        if (downloadPath == null) {
            Path dp = Aifei.getSettings().getDownloadPath();
            downloadPath = getWebRootPath().resolve(dp).toAbsolutePath().normalize();
        }
        return downloadPath;
    }

    /**
     * 获取 appHome
     */
    public static Path getAppHome() {
        return appHome;
    }

    /**
     * 获取 classPathList
     */
    public static List<Path> getClassPathList() {
        return classPathList;
    }

    /**
     * 获取 webRootPath
     */
    public static Path getWebRootPath() {
        return webRootPath;
    }

    /**
     * 获取文件上传路径。
     * 注意：必须在系统启动完之后调用才能获取到正确的值。
     */
    public static Path getUploadPath() {
        if (uploadPath == null) {
            uploadPath = resolveUploadPath();
        }
        return uploadPath;
    }

    /**
     * 获取文件下载路径。
     * 注意：必须在系统启动完之后调用才能获取到正确的值。
     */
    public static Path getDownloadPath() {
        if (downloadPath == null) {
            downloadPath = resolveDownloadPath();
        }
        return downloadPath;
    }

    /**
     * 是否为 deployMode
     * 注意：必须在系统启动完之后调用才能获取到正确的值。
     */
    public static boolean isDeployMode() {
        return deployMode;
    }

    /**
     * 是否不为 deployMode
     * 注意：必须在系统启动完之后调用才能获取到正确的值。
     */
    public static boolean notDeployMode() {
        return !deployMode;
    }

    /**
     * 去除绝对路径的根路径，返回对应的相对路径。常用于将上传、下载路径限定在特定路径及其子路径之下。
     * <pre>
     * 转换示例：
     *   /file.txt        ->  file.txt
     *   /path/file.txt   ->  path/file.txt
     *   c:/file.txt      ->  file.txt
     *   c:/path/file.txt ->  path/file.txt
     * </pre>
     */
    public static Path stripRoot(Path path) {
        if (path == null || !path.isAbsolute()) {
            return path;
        } else {
            return path.getRoot().relativize(path);
        }
    }
}




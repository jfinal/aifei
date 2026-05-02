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

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AppHome 用于解析项目在开发、部署时的根路径，部署又可分为多 jar 包部署与 fatjar 单 jar 包部署。
 *
 * <pre>
 *  Path 使用备注：
 *   1: Path.endsWith(String) 以路径片段为单位来匹配，而非路径片段的部分为单位，例如：
 *         Path path = Paths.get("abc/def");
 *         path.endsWith("def");  // 返回 true
 *         path.endsWith("f");    // 返回 false
 *
 *       文件名逻辑一样：
 *         Path path = Paths.get("abc/def.jar");
 *         path.endsWith("def.jar");  // 返回 true
 *         path.endsWith(".jar");     // 返回 false
 *
 *   2: 判断 Path 对象是否以某个文件扩展名结尾必须使用 Path.getFileName().toString().endsWith(String)
 *      而非 Path.toString().endsWith(String)，前者支持适应性更高，例如支持： "/data/a.txt/"
 * </pre>
 */
public class AppHome {

    Path appHomePath;
    Class<?> anchor;
    List<Path> classPathList;

    private AppHome(Class<?> anchor) {
        Objects.requireNonNull(anchor, "anchor can not be null.");
        this.anchor = anchor;
        this.classPathList = loadClassPathList();
        this.appHomePath = resolve().toAbsolutePath().normalize();    // 注意先 toAbsolutePath()
    }

    public static AppHome of(Class<?> anchor) {
        return new AppHome(anchor);
    }

    public Path get() {
        return appHomePath;
    }

    public List<Path> getClassPathList() {
        return classPathList;
    }

    List<Path> loadClassPathList() {
        String cp = System.getProperty("java.class.path");
        if (cp == null || cp.isEmpty()) {       // class path 有可能为 null
            return Collections.emptyList();
        }

        List<Path> classPathList = Arrays.stream(cp.split(Pattern.quote(File.pathSeparator)))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Paths::get)
                .map(Path::normalize)
                .collect(Collectors.toList());

        return Collections.unmodifiableList(classPathList);
    }

    Path resolve() {
        // 优先获取系统属性 "app.home" 与环境变量 "APP_HOME" 指定值，为用户留下定制通道
        // 可通过命令行指定，如：java -jar xxx.jar --app.home=/Users/project/aifei
        String prop = System.getProperty("app.home");
        if (notEmpty(prop)) {
            return normalize(Paths.get(prop.trim()));
        }
        String env = System.getenv("APP_HOME");
        if (notEmpty(env)) {
            return normalize(Paths.get(env.trim()));
        }

        // 通过锚定类位置解析 appHome
        Path ret = resolveByAnchor();
        if (ret != null) {
            return ret;
        }

        // 通过 class path 特征解析 appHome。注意：不要通过所有 class path 为 jar 包来判断部署环境，因为存在 config
        ret = resolveByClassPath();
        if (ret != null) {
            return ret;
        }

        // 当前 JVM 进程工作目录兜底
        return Paths.get(System.getProperty("user.dir"));
    }

    /**
     * resolveByAnchor 通过锚定类解析 appHome
     */
    Path resolveByAnchor() {
        try {
            // 获取锚定类位置
            URL url = anchor.getProtectionDomain().getCodeSource().getLocation();
            Path location = Paths.get(url.toURI()).toAbsolutePath().normalize();

            // 锚定类处在目录中，可断定处在 "非部署" 环境，向上查找项目根地标文件确定 appHome
            if (Files.isDirectory(location)) {
                return findUpwards(location, MARKERS, 5);
            }

            // 锚定类处在 jar 包之中，大概率处在 "部署" 环境中，需进一步判断 lib、config 目录的存在性
            if (location.getFileName().toString().endsWith(".jar")) {
                // 若 jar 文件处在 lib 目录下并且存在 lib 平级目录 config，则取 lib 目录上级
                Path dir = location.getParent();
                if (dir.endsWith("lib")) {
                    Path config = dir.resolveSibling("config");
                    if (Files.isDirectory(config)) {
                        return dir.getParent();
                    }
                }

                // 若 classPathList 只有一个元素，则取该 jar 文件所在目录
                if (classPathList.size() == 1) {
                    return dir;
                }
            }

            return null;
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * resolveByClassPath 通过 class path 解析 appHome
     * <pre>
     *  resolveByClassPath 设计：
     *   1: 通过 class path 特征解析 appHome。注意 class path 不一定有值。
     *   2: 只关注目录不关注 jar 文件，因为 jar 文件在 resolveByAnchor 中更好处理，检测 "部署" 环境更可靠。
     *   3: 只用于解析出 "非部署" 环境。只通过最可靠的信息来判断。
     *   4: 在判断目录之后，必须进一步判断标志文件(如 pom.xml)，因为 "部署" 环境也可以配置目录为 class path，例如 config 目录。
     * </pre>
     */
    Path resolveByClassPath() {
        // 针对所有为目录而非 jar 包的 class path，向上查找项目根地标文件确定 appHome
        for (Path classPath : classPathList) {
            if (Files.isDirectory(classPath)) {
                if (classPath.endsWith("classes")) {
                    Path ret = findUpwards(classPath, MARKERS, 5);
                    if (ret != null) {
                        return ret;
                    }
                } else if (classPath.endsWith("config")) {
                    Path lib = classPath.resolveSibling("lib");
                    if (Files.isDirectory(lib)) {
                        return classPath.getParent();
                    }
                }
            }
        }

        return null;
    }

    final String[] MARKERS = new String[]{
            "pom.xml", "build.gradle", "settings.gradle", "src", ".git", "build.gradle.kts", "settings.gradle.kts"
    };

    Path findUpwards(Path start, String[] names, int maxDepth) {
        int depth = 0;
        for (Path cur = start; cur != null && depth++ < maxDepth; cur = cur.getParent()) {
            for (String n : names) {
                if (Files.exists(cur.resolve(n))) {
                    return cur;
                }
            }
        }
        return null;
    }

    boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    Path normalize(Path path) {
        try {
            return path.toRealPath().normalize();
        } catch (Exception e) {
            return path.toAbsolutePath().normalize();
        }
    }
}




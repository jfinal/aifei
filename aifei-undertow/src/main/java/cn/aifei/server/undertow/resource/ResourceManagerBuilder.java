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

package cn.aifei.server.undertow.resource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import cn.aifei.util.PathUtil;
import cn.aifei.util.StrUtil;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceManager;

/**
 * ResourceManagerBuilder
 *
 * <pre>
 *  FileResourceManager、ClassPathResourceManager 使用
 *
 *   1：FileResourceManager 支持在普通目录下去加载 css、js 等 web 静态资源
 *
 *   2：ClassPathResourceManager 支持在 classpath 下与 jar 包内加载 css、js
 *      等 web 静态资源
 *
 *   3：ClassPathResourceManager 第二个参数不能省略，也不能为 ""，否则可以在浏览器地址栏直接访问
 *      class path、jar 中的所有资源，包括配置文件，下面的用法极其危险:
 *         new ClassPathResourceManager(classLoader);
 *
 *   4：在需要用到 ClassPathResourceManager 时为其给定一个参数，例如：
 *         new ClassPathResourceManager(getClassLoader(), "webapp"));
 *
 *      上例给定 webapp 参数，undertow 可以从 classpath、jar 包内的
 *      webapp 之下去加载 web 静态资源
 *</pre>
 */
public class ResourceManagerBuilder {

    String resourcePath;
    ClassLoader classLoader;

    public ResourceManagerBuilder(String resourcePath, ClassLoader classLoader) {
        Objects.requireNonNull(resourcePath, "resourcePath can not be null.");
        Objects.requireNonNull(classLoader, "classLoader can not be null.");

        this.resourcePath = resourcePath;
        this.classLoader = classLoader;
    }

    public ResourceManager build() {
        CompositeResourceManager ret = new CompositeResourceManager();
        List<String> resourcePathList = buildResourcePathList(resourcePath);
        buildFileResourceManager(resourcePathList, ret);
        buildClassPathResourceManager(resourcePathList, ret);
        return ret;
    }

    private List<String> buildResourcePathList(String resourcePath) {
        List<String> ret = new ArrayList<>();
        String[] resourcePathArray = resourcePath.split(",");
        for (String path : resourcePathArray) {
            if (StrUtil.notBlank(path)) {
                // 必须移除空格，支持类似 "classpath : static" 这种风格
                ret.add(path.trim().replace(" ", ""));
            }
        }

        /*
         * 提升 undertow.resourcePath 配置体验，默认添加部署与非部署环境下的两个最常用的资源路径
         * "webapp" 与 "src/main/webapp"，大多数场景可免去 undertow.resourcePath 配置
         *
         * 注意：
         *    打包在 jar 包内的资源文件仍需要添加配置，例如资源文件处在 src/main/resources/static
         *    下时需添加如下配置：
         *       undertow.resourcePath = classpath:static
         */
        if (!ret.contains("webapp")) {
            ret.add(0, "webapp");
        }
        if (!ret.contains("src/main/webapp")) {
            ret.add(1, "src/main/webapp");
        }
        return ret;
    }

    private void buildFileResourceManager(List<String> resourcePathList, CompositeResourceManager ret) {
        /*
         * 开发时使用 eclipse 启动将会正确添加 FileResourceManager
         * 执行 java -jar xxx.jar 命令时，当前目录下面如果存在 path 目录将会被添加
         *
         * 经测试 eclipse 启动项目的当前目录值为 APP_HOME
         */
        Path appHome = PathUtil.getAppHome();
        for (String path : resourcePathList) {
            Path cur = appHome.resolve(path);
            if (Files.isDirectory(cur)) {
                ret.add(createFileResourceManager(cur.toFile()));
            }
        }
    }

    private FileResourceManager createFileResourceManager(File file) {
        // false 参数提升资源获取性能，并解决 visual studio code 下的大小写问题
        // undertow 的 PathResourceManager 源代码表明配置为 false 可提升性能，后续升级 undertow 时需要关注源码
        return new FileResourceManager(file, 1024, false);
    }

    /*
     * undertow.resourcePath 中配置的 classpath:webapp 格式的值创建
     * ClassPathResourceManager 对象，从 class path 与 jar 包中读取
     * css、js 等 web 资源文件
     *
     * 建议配置：classpath:webapp 或者 classpath:static
     */
    private void buildClassPathResourceManager(List<String> resourcePathList, CompositeResourceManager ret) {
        String prefix = "classpath:";

        for (String path : resourcePathList) {
            if (path.startsWith(prefix)) {
                path = path.substring(prefix.length());
                if (StrUtil.notBlank(path)) {
                    ret.add(new ClassPathResourceManager(classLoader, path));
                }
            }
        }
    }
}


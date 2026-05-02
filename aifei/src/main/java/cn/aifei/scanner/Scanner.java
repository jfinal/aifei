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

package cn.aifei.scanner;

import cn.aifei.log.Log;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Scanner 通用类扫描，不限定用于路由扫描。
 */
public class Scanner {

    static final Log log = Log.get(Scanner.class);

    String basePackage;
    String basePackagePath;
    Predicate<Class<?>> filter;
    ClassLoader classLoader;

    Set<Class<?>> result;

    private void init(String basePackage, Predicate<Class<?>> filter) {
        if (basePackage == null || basePackage.trim().isEmpty()) {
            throw new IllegalArgumentException("basePackage can not be blank.");
        }
        if (filter == null) {
            throw new IllegalArgumentException("filter can not be null.");
        }

        if (basePackage.endsWith(".")) {
            basePackage = basePackage.substring(0, basePackage.length() - 1);
        }
        this.basePackage = basePackage;
        this.basePackagePath = basePackage.replace('.', '/');
        this.filter = filter;

        this.classLoader = Thread.currentThread().getContextClassLoader();
        if (this.classLoader == null) {
            this.classLoader = Scanner.class.getClassLoader();
        }

        this.result = new LinkedHashSet<>();
    }

    /**
     * 扫描 basePackage 指定包路径及其子包路径下的 "所有" 类文件。一般建议使用带 filter 参数 scan 方法。
     */
    public Set<Class<?>> scan(String basePackage) {
        return scan(basePackage, clazz -> true);
    }

    /**
     * 扫描 basePackage 指定包路径及其子包路径下的类文件，并使用 filter 函数进行过滤
     *
     * <pre>
     *  scan 设计：
     *   1: basePackage 用于指定被抛描的包及其子包。
     *   2: filter 函数可用于过滤出需要的类文件输出到 scan 方法的返回值中。
     *   3: filter 函数还可以直接将所扫描到的类按不同的类型分别保存、处理，通过一次扫描即可触达所有类文件。
     *
     *  filter 例子：
     *    // 过滤出 Implement 注解过的类
     *    Set<Class<?>> ret = new Scanner().scan("cn.aifei.vip", c -> c.isAnnotationPresent(Implement.class));
     *
     *  filter 例子：
     *    // 通过 filter 函数同时获取不同类别 class 并分别存放
     *    Set<Class<?>> serviceSet = new HashSet<>();
     *    Set<Class<?>> implementSet = new HashSet<>();
     *
     *    new Scanner().scan("cn.aifei.vip", c -> {
     *        if (Service.class.isAssignableFrom(c)) {
     *            // 扫描获取 Service 的子类
     *            serviceSet.add(c);
     *        } else if (c.isAnnotationPresent(Implement.class)) {
     *            // 扫描获取 Implement 注解过的类
     *            implementSet.add(c);
     *        }
     *
     *        // 返回 false 不输出到 scan 方法返回值
     *        return false;
     *    });
     * </pre>
     */
    public Set<Class<?>> scan(String basePackage, Predicate<Class<?>> filter) {
        init(basePackage, filter);

        try {
            Enumeration<URL> resources = classLoader.getResources(basePackagePath);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getPath(), "UTF-8");
                    scanDirectory(new File(filePath), this.basePackage);
                } else if ("jar".equals(protocol)) {
                    scanJar(url);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to scan classes for base package: " + this.basePackage, e);
        }

        return result;
    }

    private void scanDirectory(File dir, String pkgName) {
        if (!dir.exists()) {return;}
        File[] files = dir.listFiles();
        if (files == null) {return;}

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, pkgName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = pkgName + '.' + file.getName().substring(0, file.getName().length() - 6);
                handleClass(className);
            }
        }
    }

    private void scanJar(URL url) throws IOException {
        URLConnection urlConn = url.openConnection();
        if (urlConn instanceof JarURLConnection) {
            JarURLConnection jarConn = (JarURLConnection) urlConn;
            try (JarFile jarFile = jarConn.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(basePackagePath) && name.endsWith(".class")) {
                        String className = name.substring(0, name.length() - 6).replace('/', '.');
                        handleClass(className);
                    }
                }
            }
        } else {
            log.debug("Cannot scan types other than JarURLConnection : " + urlConn.getClass());
        }
    }

    private void handleClass(String className) {
        try {
            // false 参数避免触发类初始化，例如避免执行静态代码块
            Class<?> clazz = Class.forName(className, false, classLoader);
            if (filter.test(clazz)) {
                result.add(clazz);
            }

        } catch (Throwable t) { // 必需处理 NoClassDefFoundError
            log.debug("Failed to load class: " + className, t);
        }
    }
}




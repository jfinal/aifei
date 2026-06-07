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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * PropKit. 用于从 CLASSPATH 或指定文件加载 properties 配置文件.
 */
public class PropKit {

    private static String activeProfilesKey = "aifei.profiles.active";

    private static Prop prop;
    private static final ConcurrentHashMap<String, Prop> cache = new ConcurrentHashMap<>();

    private PropKit() {}

    /**
     * 设置用于获取当前激活环境（profile）的配置项 key 名称。
     * 默认值为 "aifei.profiles.active"。
     */
    public static void setActiveProfilesKey(String activeProfilesKey) {
        if (StrUtil.isBlank(activeProfilesKey)) {
            throw new IllegalArgumentException("activeProfilesKey can not be blank.");
        }
        PropKit.activeProfilesKey = activeProfilesKey.trim();
    }

    public static String getActiveProfilesKey() {
        return PropKit.activeProfilesKey;
    }

    /**
     * Returns parsed active profiles.
     * e.g. ["dev", "prod"]
     */
    public static List<String> getActiveProfiles(Prop prop) {
        // 优先从系统变量中获取，支持命令行传参 --aifei.profiles.active=pro
        // PropKit.prop 判断 null 值：仅第一次调用从系统变量中获取 activeProfiles，否则从 prop 中获取
        String value = (PropKit.prop == null ? System.getProperty(activeProfilesKey) : null);
        if (StrUtil.isBlank(value)) {
            value = prop.get(activeProfilesKey);
            if (StrUtil.notBlank(value)) {
                // 放入系统变量，供其它地方使用，如 UndertowConfig
                System.setProperty(activeProfilesKey, value.trim());
            }
        }

        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        } else {
            return Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
    }

    public static List<String> getActiveProfiles() {
        return getActiveProfiles(getProp());
    }

    /**
     * Use the properties file. It will load the properties file if not loading.
     * @see #use(String, String)
     */
    public static Prop use(String fileName) {
        return use(fileName, Prop.DEFAULT_ENCODING);
    }

    /**
     * Use the properties file. It will load the properties file if not loading.
     * <p>
     * Example:<br>
     * PropKit.use("config.txt", "UTF-8");<br>
     * PropKit.use("other_config.txt", "UTF-8");<br><br>
     * String userName = PropKit.get("userName");<br>
     * String password = PropKit.get("password");<br><br>
     *
     * userName = PropKit.use("other_config.txt").get("userName");<br>
     * password = PropKit.use("other_config.txt").get("password");<br><br>
     *
     * PropKit.use("cn/aifei/config_in_sub_directory_of_classpath.txt");
     *
     * @param fileName the properties file's name in classpath or the subdirectory of classpath
     * @param encoding the encoding
     */
    public static Prop use(String fileName, String encoding) {
        return cache.computeIfAbsent(fileName, key -> {
            Prop ret = new Prop(key, encoding);
            handleActiveProfiles(ret, key);
            if (PropKit.prop == null) {
                PropKit.prop = ret;
            }
            return ret;
        });
    }

    /**
     * 根据当前激活环境（profile）配置追加配置到当前 Prop 对象之中，便于项目在 dev、pro 等等
     * 在不同环境下融合、切换配置文件
     *
     * <pre>
     * 用法：
     *  1： 假定 config.txt 中存在配置 aifei.profiles.active = pro
     *  2： PropKit.use("config.txt") 则会加载 config.txt 并追加 config-pro.txt 配置
     *  3:  可同时配置多个激活环境，如：aifei.profiles.active = dev, pro
     * </pre>
     */
    private static void handleActiveProfiles(Prop result, String fileName) {
        List<String> activeProfiles = getActiveProfiles(result);
        if (!activeProfiles.isEmpty()) {
            int index = fileName.lastIndexOf('.');
            String mainName = fileName.substring(0, index);
            String extName = fileName.substring(index);
            for (String activeProfile : activeProfiles) {
                String activeFile = mainName + "-" + activeProfile + extName;
                Prop activeProp = new Prop(activeFile);
                result.append(activeProp);
            }
        }
    }

    /**
     * Use the properties file bye File object. It will load the properties file if not loading.
     * @see #use(File, String)
     */
    public static Prop use(File file) {
        return use(file, Prop.DEFAULT_ENCODING);
    }

    /**
     * Use the properties file bye File object. It will load the properties file if not loading.
     * <p>
     * Example:<br>
     * PropKit.use(new File("/var/config/my_config.txt"), "UTF-8");<br>
     * Strig userName = PropKit.use("my_config.txt").get("userName");
     *
     * @param file the properties File object
     * @param encoding the encoding
     */
    public static Prop use(File file, String encoding) {
        return cache.computeIfAbsent(file.getName(), key -> {
            Prop ret = new Prop(file, encoding);
            handleActiveProfiles(ret, key);
            if (PropKit.prop == null) {
                PropKit.prop = ret;
            }
            return ret;
        });
    }

    public static Prop useless(String fileName) {
        Prop previous = cache.remove(fileName);
        if (PropKit.prop == previous) {
            PropKit.prop = null;
        }
        return previous;
    }

    public static void clear() {
        prop = null;
        cache.clear();
    }

    public static Prop append(Prop prop) {
        synchronized (PropKit.class) {
            if (PropKit.prop != null) {
                PropKit.prop.append(prop);
            } else {
                PropKit.prop = prop;
            }
            return PropKit.prop;
        }
    }

    public static Prop append(String fileName, String encoding) {
        return append(new Prop(fileName, encoding));
    }

    public static Prop append(String fileName) {
        return append(fileName, Prop.DEFAULT_ENCODING);
    }

    public static Prop appendIfExists(String fileName, String encoding) {
        try {
            return append(new Prop(fileName, encoding));
        } catch (Exception e) {
            return PropKit.prop;
        }
    }

    public static Prop appendIfExists(String fileName) {
        return appendIfExists(fileName, Prop.DEFAULT_ENCODING);
    }

    public static Prop append(File file, String encoding) {
        return append(new Prop(file, encoding));
    }

    public static Prop append(File file) {
        return append(file, Prop.DEFAULT_ENCODING);
    }

    public static Prop appendIfExists(File file, String encoding) {
        if (file.exists()) {
            append(new Prop(file, encoding));
        }
        return PropKit.prop;
    }

    public static Prop appendIfExists(File file) {
        return appendIfExists(file, Prop.DEFAULT_ENCODING);
    }

    /**
     * Use the first found properties file
     */
    public static Prop useFirstFound(String... fileNames) {
        for (String fn : fileNames) {
            try {
                return use(fn, Prop.DEFAULT_ENCODING);
            } catch (Exception ignored) {
            }
        }
        throw new IllegalArgumentException("没有配置文件可被使用");
    }

    public static Prop getProp() {
        if (prop == null) {
            throw new IllegalStateException("Load properties file by invoking PropKit.use(String fileName) method first.");
        }
        return prop;
    }

    public static Prop getProp(String fileName) {
        return cache.get(fileName);
    }

    public static String get(String key) {
        return getProp().get(key);
    }

    public static String get(String key, String defaultValue) {
        return getProp().get(key, defaultValue);
    }

    public static Integer getInt(String key) {
        return getProp().getInt(key);
    }

    public static Integer getInt(String key, Integer defaultValue) {
        return getProp().getInt(key, defaultValue);
    }

    public static Long getLong(String key) {
        return getProp().getLong(key);
    }

    public static Long getLong(String key, Long defaultValue) {
        return getProp().getLong(key, defaultValue);
    }

    public static Double getDouble(String key) {
        return getProp().getDouble(key);
    }

    public static Double getDouble(String key, Double defaultValue) {
        return getProp().getDouble(key, defaultValue);
    }

    public static Boolean getBoolean(String key) {
        return getProp().getBoolean(key);
    }

    public static Boolean getBoolean(String key, Boolean defaultValue) {
        return getProp().getBoolean(key, defaultValue);
    }

    public static boolean containsKey(String key) {
        return getProp().containsKey(key);
    }
}



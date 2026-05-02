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

package cn.aifei.server.undertow.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Properties;

/**
 * PropExt
 *
 * 支持 undertow 从 config 目录以及 jar 包中读取配置文件
 */
public class PropExt {

    static final String DEFAULT_ENCODING = "UTF-8";

    protected Properties properties;

    /**
     * 启用 jfinal 原有 Prop.getClassLoader()
     */
    private ClassLoader getClassLoader() {
        ClassLoader ret = Thread.currentThread().getContextClassLoader();
        return ret != null ? ret : getClass().getClassLoader();
    }

    /**
     * 支持 new PropExt().appendIfExists(...);
     */
    public PropExt() {
        properties = new Properties();
    }

    public PropExt(Properties properties) {
        this.properties = properties;
    }

    /**
     * PropExt constructor.
     * @see #PropExt(String, String)
     */
    public PropExt(String fileName) {
        this(fileName, DEFAULT_ENCODING);
    }

    /**
     * PropExt constructor
     * <p>
     * Example:<br>
     * PropExt prop = new PropExt("my_config.txt", "UTF-8");<br>
     * String userName = prop.get("userName");<br><br>
     *
     * prop = new PropExt("com/jfinal/file_in_sub_path_of_classpath.txt", "UTF-8");<br>
     * String value = prop.get("key");
     *
     * @param fileName the properties file's name in classpath or the subdirectory of classpath
     * @param encoding the encoding
     */
    public PropExt(String fileName, String encoding) {
        InputStream inputStream = null;
        try {
            inputStream = getClassLoader().getResourceAsStream(fileName);		// properties.load(PropExt.class.getResourceAsStream(fileName));
            if (inputStream == null) {
                throw new PropFileNotFoundException("Properties file not found in classpath: " + fileName);
            }
            properties = new Properties();
            properties.load(new InputStreamReader(inputStream, encoding));

        } catch (IOException e) {
            throw new RuntimeException("Error loading properties file.", e);
        } finally {
            if (inputStream != null) try {inputStream.close();} catch (IOException ignored) {}
        }
    }

    /**
     * PropExt constructor.
     * @see #PropExt(File, String)
     */
    public PropExt(File file) {
        this(file, DEFAULT_ENCODING);
    }

    /**
     * PropExt constructor
     * <p>
     * Example:<br>
     * PropExt prop = new PropExt(new File("/var/config/my_config.txt"), "UTF-8");<br>
     * String userName = prop.get("userName");
     *
     * @param file the properties File object
     * @param encoding the encoding
     */
    public PropExt(File file, String encoding) {
        if (file == null) {
            throw new IllegalArgumentException("File can not be null.");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("File not found : " + file.getName());
        }

        InputStream inputStream = null;
        try {
            inputStream = Files.newInputStream(file.toPath());
            properties = new Properties();
            properties.load(new InputStreamReader(inputStream, encoding));

        } catch (IOException e) {
            throw new RuntimeException("Error loading properties file.", e);
        } finally {
            if (inputStream != null) try {inputStream.close();} catch (IOException ignored) {}
        }
    }

    public PropExt append(PropExt prop) {
        if (prop == null) {
            throw new IllegalArgumentException("prop can not be null");
        }
        properties.putAll(prop.getProperties());
        return this;
    }

    public PropExt append(String fileName, String encoding) {
        return append(new PropExt(fileName, encoding));
    }

    public PropExt append(String fileName) {
        return append(fileName, DEFAULT_ENCODING);
    }

    public PropExt appendIfExists(String fileName, String encoding) {
        try {
            return append(new PropExt(fileName, encoding));
        } catch (Exception ignored) {
            return this;
        }
    }

    public PropExt appendIfExists(String fileName) {
        return appendIfExists(fileName, DEFAULT_ENCODING);
    }

    public String get(String key) {
        // 下面这行代码只要 key 存在，就不会返回 null。未给定 value 或者给定一个或多个空格都将返回 ""
        String value = properties.getProperty(key);
        return value != null && !value.isEmpty() ? value.trim() : null;
    }

    public String get(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value != null && !value.isEmpty() ? value.trim() : defaultValue;
    }

    public Integer getInt(String key) {
        return getInt(key, null);
    }

    public Integer getInt(String key, Integer defaultValue) {
        String value = get(key);
        return value != null ? Integer.valueOf(value) : defaultValue;
    }

    public Long getLong(String key) {
        return getLong(key, null);
    }

    public Long getLong(String key, Long defaultValue) {
        String value = get(key);
        return value != null ? Long.valueOf(value) : defaultValue;
    }

    public Double getDouble(String key) {
        return getDouble(key, null);
    }

    public Double getDouble(String key, Double defaultValue) {
        String value = get(key);
        return value != null ? Double.valueOf(value) : defaultValue;
    }

    public Boolean getBoolean(String key) {
        return getBoolean(key, null);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        String value = get(key);
        if (value != null) {
            value = value.toLowerCase();
            if ("true".equals(value)) {
                return true;
            } else if ("false".equals(value)) {
                return false;
            }
            throw new RuntimeException("The value can not parse to Boolean : " + value);
        }
        return defaultValue;
    }

    public boolean containsKey(String key) {
        return properties.containsKey(key);
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    public boolean notEmpty() {
        return ! properties.isEmpty();
    }

    public Properties getProperties() {
        return properties;
    }

    // ---------

    public static class PropFileNotFoundException extends RuntimeException {
        public PropFileNotFoundException(String msg) {
            super(msg);
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }
}


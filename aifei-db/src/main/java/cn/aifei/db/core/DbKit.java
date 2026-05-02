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

package cn.aifei.db.core;

import cn.aifei.db.ext.NullDataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * DbKit
 */
public class DbKit {

    public static final String MAIN_CONFIG_ID = "main";
    public static final String FAKE_CONFIG_ID = "fake";

    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    static final DbConfig fakeConfig = new DbConfig(FAKE_CONFIG_ID, NullDataSource.instance);

    // 主配置
    static DbConfig config = fakeConfig;

    // configId 到 DbConfig 映射
    private static final Map<String, DbConfig> configIdToConfig = new HashMap<>(32);

    // Model 到 DbConfig 映射
    private static final Map<Class<? extends AifeiRow<?>>, DbConfig> modelToConfig = new HashMap<>(512);

    private static volatile boolean dbUseThreadLocalConfig = false;
    private static final ThreadLocal<DbConfig> threadLocalConfig = new ThreadLocal<>();

    private DbKit() {}

    /**
     * 获取主配置
     */
    public static DbConfig getConfig() {
        if (dbUseThreadLocalConfig) {
            DbConfig ret = threadLocalConfig.get();
            return ret != null ? ret : config;
        } else {
            return config;
        }
    }

    public static DbConfig getConfig(String configId) {
        return configIdToConfig.get(configId);
    }

    public static DbConfig getConfig(Class<? extends AifeiRow<?>> modelClass) {
        return modelToConfig.get(modelClass);
    }

    /**
     * 添加配置
     */
    public synchronized static void addConfig(DbConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("DbConfig can not be null");
        }
        if (configIdToConfig.containsKey(config.getId())) {
            throw new IllegalArgumentException("DbConfig already exists: " + config.getId());
        }

        configIdToConfig.put(config.getId(), config);

        /*
         * Replace the main config if current config name is MAIN_CONFIG_ID
         */
        if (MAIN_CONFIG_ID.equals(config.getId())) {
            DbKit.config = config;
        }

        /*
         * The configId may not be MAIN_CONFIG_ID,
         * the main config have to set the first coming Config
         * if it is null or fakeConfig
         */
        if (DbKit.config == null || DbKit.config == fakeConfig) {
            DbKit.config = config;
        }
    }

    /**
     * 移除配置
     */
    public synchronized static DbConfig removeConfig(String configId) {
        if (DbKit.config != null && DbKit.config.getId().equals(configId)) {
            DbKit.config = null;
        }

        return configIdToConfig.remove(configId);
    }

    static void addModelToConfigMapping(Class<? extends AifeiRow<?>> modelClass, DbConfig config) {
        modelToConfig.put(modelClass, config);
    }

    /**
     * 开启支持 Db.use() 方法在当前线程中共享 DbConfig 对象。可实现在拦截器中统一切换数据源，
     * 适用数据库多租户等应用场景。
     * <p>
     * 注意：这里当前线程仅仅共享了同一个 DbConfig 对象，而不是共享同一个 Connection 对象
     */
    public static void setDbUseThreadLocalConfig(boolean enable) {
        DbKit.dbUseThreadLocalConfig = enable;
    }

    public static void setThreadLocalConfig(DbConfig config) {
        threadLocalConfig.set(config);
    }

    public static DbConfig getThreadLocalConfig() {
        return threadLocalConfig.get();
    }

    public static void removeThreadLocalConfig() {
        threadLocalConfig.remove();
    }
}



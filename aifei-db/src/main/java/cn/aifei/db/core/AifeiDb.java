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

import cn.aifei.db.dialect.Dialect;
import cn.aifei.db.ext.AifeiRowFieldGetter;
import cn.aifei.db.ext.NullDataSource;
import cn.aifei.db.hook.DbHookKit;
import cn.aifei.db.sql.SqlSource;
import cn.aifei.enjoy.util.StrUtil;
import cn.aifei.enjoy.Engine;
import cn.aifei.log.Log;
import cn.aifei.log.LogKit;
import cn.aifei.log.NoLogFactory;
import javax.sql.DataSource;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * AifeiDb 为 aifei-db 系统启动入口
 */
public class AifeiDb {

    static {
        // 支持 Enjoy 属性访问表达式获取 AifeiRow 对象中的数据，全局生效
        Engine.addFieldGetterToLast(new AifeiRowFieldGetter());

        // AifeiDao 子类具有状态不能被注入，通过 onCreateProxy 避免其被注入，确保线程安全
        try {
            cn.aifei.aop.AopKit.get().onCreateProxy(type -> {
                if (AifeiDao.class.isAssignableFrom(type)) {
                    throw new UnsupportedOperationException("AifeiDao subclasses must not be injected or proxied (stateful): " + type.getName());
                }
                return type;
            });
        } catch (Exception ignored) {}
    }

    private volatile boolean isStarted = false;
    private final DbConfig config;
    private final Set<Class<? extends AifeiRow<?>>> modelSet = new LinkedHashSet<>();

    /**
     * AifeiDb 构造
     *
     * @param configId   配置 id
     * @param dataSource 数据源
     */
    public AifeiDb(String configId, DataSource dataSource) {
        if (StrUtil.isBlank(configId)) {
            throw new IllegalArgumentException("configId can not be blank.");
        }
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource can not be null.");
        }
        this.config = new DbConfig(configId, dataSource);
    }

    /**
     * AifeiDb 构造
     *
     * @param configId   配置 id
     * @param dataSourceSupplier 数据源提供函数
     */
    public AifeiDb(String configId, Supplier<DataSource> dataSourceSupplier) {
        this(configId, dataSourceSupplier.get());
    }

    /**
     * 支持无数据库场景，例如仅将 model 当成 Java Bean 使用的场景。
     * 例：new AifeiDb().addModelSet(...).start();
     */
    public AifeiDb() {
        this(DbKit.FAKE_CONFIG_ID, NullDataSource.instance);
    }

    public AifeiDb setDialect(Dialect dialect) {
        config.setDialect(dialect);
        return this;
    }

    /**
     * 配置是否开启 sql 打印功能，以及是否打印 sql 到日志。默认值都为 false。
     */
    public AifeiDb setPrintSql(boolean printSql, boolean printSqlToLog) {
        config.setPrintSql(printSql, printSqlToLog);
        return this;
    }

    /**
     * 配置是否开启 sql 打印功能，默认值为 false。
     */
    public AifeiDb setPrintSql(boolean printSql) {
        config.setPrintSql(printSql);
        return this;
    }

    /**
     * 配置是否打印 sql 到日志，默认值为 false。
     */
    public AifeiDb setPrintSqlToLog(boolean printSqlToLog) {
        config.setPrintSqlToLog(printSqlToLog);
        return this;
    }

    /**
     * 配置是否格式化被打印的 sql，默认值为 false。开发环境配置为 true 提升开发体验。
     */
    public AifeiDb setFormatSql(boolean formatSql) {
        config.setFormatSql(formatSql);
        return this;
    }

    /**
     * 配置 DbConfig
     */
    public AifeiDb config(Consumer<DbConfig> consumer) {
        consumer.accept(config);
        return this;
    }

    /**
     * 获取 DbConfig 对象。
     *
     * <pre>
     * 例如：
     *     aifeiDb.getConfig().setOnException(e -> {
     *         return Out.fail("事务执行失败，请联系管理员: " + e.getMessage());
     *     });
     * <pre/>
     */
    public DbConfig getConfig() {
        return config;
    }

    /**
     * 添加 model 到当前 AifeiDb 中，将 Model 集合关联到 DbConfig，从而关联到数据源。
     * <p>
     * 注意：将 ModelSetGenerator 生成器生成的 ModelSet 对象作为参数调用该方法，避免手工一个一个添加
     */
    public AifeiDb addModelSet(Set<Class<? extends AifeiRow<?>>> modelSet) {
        this.modelSet.addAll(modelSet);
        return this;
    }

    /**
     * 添加 model 到当前 AifeiDb 中，将 Model 关联到 DbConfig，从而关联到数据源
     */
    public AifeiDb addModel(Class<? extends AifeiRow<?>> model) {
        this.modelSet.add(model);
        return this;
    }

    /**
     * 获取 Enjoy Sql Engine
     */
    public Engine getEngine() {
        return config.sqlKit.getEngine();
    }

    /**
     * 配置 Enjoy sql 文件基础路径
     */
    public AifeiDb setBaseSqlFilePath(String baseSqlFilePath) {
        config.sqlKit.setBaseSqlFilePath(baseSqlFilePath);
        return this;
    }

    /**
     * 配置是否开启外部 sql 文件热加载（仅对 AifeiDb.addSqlFile 添加的文件有效）。默认值 false。
     *
     * <pre>
     * 热加载逻辑：
     * 1: ClassPathSourceFactory 为 Engine 的默认配置，所以默认不支持热加载，需配置为 FileSourceFactory 才支持。
     * 2: 所谓外部 sql 文件热加载，是指在程序运行时，外部 sql 文件内容被修改后是否重新加载、解析、生效。
     * 3: 仅支持 getSqlParaById(...) 进行热加载，该方法对应外部文中的 sql。
     * 4: 不支持 getSqlPara(...) 的热加载，该方法对应 Java 源码内的 sql。
     *</pre>
     */
    public AifeiDb setSqlFileHotReloading(boolean enable) {
        config.sqlKit.setSqlFileHotReloading(enable);
        return this;
    }

    /**
     * 通过外部文件添加 Enjoy sql
     * 注意：sql 内容 "需要" 包含 #sql 指令
     *
     * @param sqlFile Enjoy sql 文件
     */
    public AifeiDb addSqlFile(String sqlFile) {
        config.sqlKit.addSqlFile(sqlFile);
        return this;
    }

    /**
     * 通过 String sql 添加 Enjoy sql
     */
    public AifeiDb addSql(String sqlId, String sql) {
        config.sqlKit.addSql(sqlId, sql);
        return this;
    }

    /**
     * 通过 SqlSource 接口添加 Enjoy sql
     */
    public AifeiDb addSql(SqlSource sqlSource) {
        config.sqlKit.addSql(sqlSource);
        return this;
    }

    /**
     * 配置 DbHookKit
     *
     * <pre>
     * 举例：
     *   aifeiDb.configDbHookKit(hookKit -> {
     *       hookKit.setInsertHook(new MyInsertHook());     // 自定义 MyInsertHook
     *       hookKit.setDeleteHook(new MyDeleteHook());     // 自定义 MyDeleteHook
     *       hookKit.setUpdateHook(new MyUpdateHook());     // 自定义 MyUpdateHook
     *   });
     * </pre>
     */
    public AifeiDb configDbHookKit(Consumer<DbHookKit> hookKit) {
        config.configDbHookKit(hookKit);
        return this;
    }

    public void start() {
        if (isStarted) {
            return;
        }

        checkLogSupport();              // 检测日志支持
        config.sqlKit.parseSqlFile();   // 解析 enjoy sql 文件
        DbKit.addConfig(config);        // 添加 config
        mappingModelsToDbConfig();      // 映射 models 到 config
        isStarted = true;
    }

    public void stop() {
        DbKit.removeConfig(config.getId());
        isStarted = false;
    }

    /**
     * 检测日志支持。
     * <p>
     * aifei db 对日志的依赖只有 aifei-log，需明确引入 aifei-slf4j 或者 aifei-log4j2 依赖并提供相应的配置文件，
     * aifei-log 会自动检测配置文件并初始化日志实现。可通过 LogKit.get().setLogFactory(...) 配置指定的日志实现。
     */
    private void checkLogSupport() {
        if (LogKit.get().getLogFactory() instanceof NoLogFactory) {
            throw new RuntimeException("The logging function is not supported, import the logging library first!");
        }
        Log.get(AifeiDb.class).trace("checkLogSupport()");
    }

    /**
     * model 映射到当前 AifeiDb 的 DbConfig，并且绑定数据源
     */
    private void mappingModelsToDbConfig() {
        for (Class<? extends AifeiRow<?>> modelClass : modelSet) {
            DbKit.addModelToConfigMapping(modelClass, config);
        }
    }
}





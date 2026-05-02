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

package cn.aifei.db.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import cn.aifei.db.core.SqlPara;
import cn.aifei.enjoy.util.StrUtil;
import cn.aifei.enjoy.Engine;
import cn.aifei.enjoy.Template;

/**
 * SqlKit
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class SqlKit {

    static final String SQL_CACHE_KEY = "_SQL_CACHE_";
    public static final String SQL_PARA_KEY = "_SQL_PARA_";
    public static final String PARA_ARRAY_KEY = "_PARA_ARRAY_"; // 此参数保持不动，已被用于模板取值 _PARA_ARRAY_[n]

    private final String configName;
    private final Engine engine;

    // getSqlParaById 支持外部 sql 文件
    private final List<String> sqlFileList = new ArrayList<>();
    private final Map<String, Template> sqlFromSqlFile = new ConcurrentHashMap<>();

    // getSqlPara、与 getSqlParaById 共享缓存对象 cache
    private final Map<String, Template> cache = new ConcurrentHashMap<>(1024);

    public SqlKit(String configName) {
        this(configName, false);
    }

    private SqlKit(String configName, boolean sqlFileHotReloading) {
        this.configName = configName;

        engine = new Engine(configName);
        engine.setDevMode(sqlFileHotReloading);
        engine.setToClassPathSourceFactory();

        engine.addDirective("sql", SqlDirective.class, false);

        // 新增三条指令 #where、#and、#orderBy
        engine.addDirective("where", WhereDirective.class, true);
        engine.addDirective("and", AndDirective.class, true);
        engine.addDirective("orderBy", OrderByDirective.class, true);

        // 建议使用 #where、#and 替代 #para、#p 指令
        engine.addDirective("para", ParaDirective.class, true);
        engine.addDirective("p", ParaDirective.class, true);        // 配置 #para 指令的别名指令 #p，不建议使用，在此仅为兼容 3.0 版本
    }

    public Engine getEngine() {
        return engine;
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
    public void setSqlFileHotReloading(boolean enable) {
        engine.setDevMode(enable);
    }

    /**
     * 配置 Enjoy sql 文件基础路径
     */
    public void setBaseSqlFilePath(String baseSqlFilePath) {
        engine.setBaseTemplatePath(baseSqlFilePath);
    }

    /**
     * 通过外部文件添加 Enjoy sql
     * 注意：sql 内容 "需要" 包含 #sql 指令
     *
     * @param sqlFile Enjoy sql 文件
     */
    public void addSqlFile(String sqlFile) {
        if (StrUtil.isBlank(sqlFile)) {
            throw new IllegalArgumentException("sqlFile can not be blank");
        }
        sqlFileList.add(sqlFile);
    }

    /**
     * 通过 String sql 添加 Enjoy sql
     */
    public void addSql(String sqlId, String sql) {
        if (StrUtil.isBlank(sqlId)) {
            throw new IllegalArgumentException("sqlId can not be blank");
        }
        if (StrUtil.isBlank(sql)) {
            throw new IllegalArgumentException("sql can not be blank");
        }

        // 直接解析，而非添加到 list
        getSqlPara(sqlId, sql);
    }

    /**
     * 通过 SqlSource 接口添加 Enjoy sql
     */
    public void addSql(SqlSource sqlSource) {
        if (sqlSource == null) {
            throw new IllegalArgumentException("sqlSource can not be null");
        }
        if (sqlSource.getId() == null) {
            throw new IllegalArgumentException("sqlId of sqlSource can not be null");
        }
        if (sqlSource.getSql() == null) {
            throw new IllegalArgumentException("sql of sqlSource can not be null");
        }

        // 直接解析，而非添加到 list
        getSqlPara(sqlSource.getId(), sqlSource.getSql());
    }

    public synchronized void parseSqlFile() {
        // Map<String, Template> sqlTemplateMap = new HashMap<>(1024);

        // 解析存放在 sqlFileList 中的外部 sql 文件，全部存入 sqlFromSqlFile
        for (String sqlFile : sqlFileList) {
            Template template = engine.getTemplate(sqlFile);
            Map<Object, Object> data = new HashMap<>();
            data.put(SQL_CACHE_KEY, sqlFromSqlFile);
            template.renderToString(data);
        }

        // 检查 sqlId 是否存在，避免热加载开启时 sql 被覆盖，造成安全隐患
        for (String sqlId : sqlFromSqlFile.keySet()) {
            if (cache.containsKey(sqlId)) {
                throw new IllegalArgumentException("sqlId already exists: " + sqlId);
            }
        }

        // 将 sqlFromSqlFile 中的解析结果放入缓存 cache
        cache.putAll(sqlFromSqlFile);

        // this.sqlTemplateMap = sqlTemplateMap;
    }

    /**
     * sqlFromSqlFile 变量作用：
     * 1: 存放外部 sql 文件生成的 sql。用于实现热加载时从 cache 中仅仅移除外部文件 sql，否则只能移除 cache 中所有 sql，
     *    造成 Db.sql(...) 缓存的 sql 失效，从而造成 Db.sql("|findUser|") 这种用法找不到 sql 而抛出异常
     *
     * 2: 用于判断外部 sql 模板是否被修改：取出 sqlFromSqlFile 中的模板并调用 template.isModified() 来实现
     *
     * 3: 热加载重新解析 sql 文件时，用于判断 sqlId 是否存在，避免热加载开启时 sql 被覆盖，造成安全隐患
     */
    private void reloadModifiedSqlTemplate() {
        // 去除 Engine 中的缓存，以免 get 出来后重新判断 isModified
        engine.removeAllTemplateCache();
        // 精准移除 cache 中来自 AifeiDb.addSqlFile(...) 所缓存的 sql，保留来自 Db.sql(...)、AifeiDb.addSql(...) 所缓存的 sql。
        sqlFromSqlFile.forEach((sqlId, sql) -> cache.remove(sqlId));
        // 清空以便在 parseSqlFile() 再次使用
        sqlFromSqlFile.clear();

        parseSqlFile();
    }

    private boolean isSqlTemplateModified() {
        for (Template template : sqlFromSqlFile.values()) {
            if (template.isModified()) {
                return true;
            }
        }
        return false;
    }

    // 暂不支持 Enjoy 模板文件热加载，原因是：
    //   1：通过 Db.sql、Db.sqlId 产生的 enjoy sql 被缓存后，热加载会影响到 Map cache，从而影响到这批 sql
    //   2：通过 AifeiDb.addSql(...) 添加的 enjoy sql 可能也受影响
    private Template getSqlTemplate(String sqlId) {
        Template template = cache.get(sqlId);
        if (template == null) {    // 此 if 分支，处理起初没有定义，但后续不断追加 sql 的情况
            if (!engine.getDevMode()) {
                return null;
            }
            if (isSqlTemplateModified()) {
                synchronized (this) {
                    if (isSqlTemplateModified()) {
                        reloadModifiedSqlTemplate();
                        template = cache.get(sqlId);
                    }
                }
            }
            return template;
        }

        if (engine.getDevMode() && template.isModified()) {
            synchronized (this) {
                template = cache.get(sqlId);
                if (template.isModified()) {
                    reloadModifiedSqlTemplate();
                    template = cache.get(sqlId);
                }
            }
        }
        return template;
    }

    // /**
    //  * 通过 sqlId 获取 sql
    //  */
    // public String getSql(String sqlId) {
    //     return getSql(sqlId, null);
    // }

    // /**
    //  * 通过 sqlId 获取 sql
    //  * 传入变量 Map data 参与 sql 生成
    //  * 警告：变量值如果来自用户输入，需避免被 sql 注入
    //  */
    // public String getSql(String sqlId, Map data) {
    //     Template template = getSqlTemplate(sqlId);
    //     return template != null ? template.renderToString(data) : null;
    // }

    /**
     * 通过 sqlId 获取 SqlPara 对象
     * <pre>
     *    例子：
     *    1：sql 定义
     *      #sql("sqlId")
     *          select * from xxx where id = #para(id) and age > #para(age)
     *      #end
     * <p>
     *    2：java 代码
     *       Kv cond = Kv.of("id", 123).set("age", 18);
     *       getSqlParaById("sqlId", cond);
     * </pre>
     */
    public SqlPara getSqlParaById(String sqlId, Map data) {
        // Template template = cache.get(sqlId);
        Template template = getSqlTemplate(sqlId);
        if (template == null) {
            return null;
        }

        SqlPara sqlPara = new SqlPara().setId(sqlId);
        data.put(SQL_PARA_KEY, sqlPara);
        sqlPara.setSql(template.renderToString(data));
        data.remove(SQL_PARA_KEY);    // 避免污染传入的 Map
        return sqlPara;
    }

    /**
     * 通过 sqlId 获取 SqlPara 对象
     * <pre>
     * 例子：
     *    1：sql 定义
     *       #sql("sqlId")
     *           select * from xxx where a = #para(0) and b = #para(1)
     *       #end
     *
     *    2：java 代码
     *       getSqlParaById("sqlId", 123, 456);
     * </pre>
     */
    public SqlPara getSqlParaById(String sqlId, Object... paras) {
        // Template template = cache.get(sqlId);
        Template template = getSqlTemplate(sqlId);
        if (template == null) {
            return null;
        }

        SqlPara sqlPara = new SqlPara().setId(sqlId);
        Map data = new HashMap();
        data.put(SQL_PARA_KEY, sqlPara);
        data.put(PARA_ARRAY_KEY, paras);
        sqlPara.setSql(template.renderToString(data));
        // data 为本方法中创建，不会污染用户数据，无需移除 SQL_PARA_KEY、PARA_ARRAY_KEY
        return sqlPara;
    }

    public java.util.Set<Map.Entry<String, Template>> getSqlMapEntrySet() {
        return cache.entrySet();
    }

    public String toString() {
        return "SqlKit for config : " + configName;
    }

    // ------------------------------------------------------------------------------

    /**
     * 通过 String 内容获取 SqlPara 对象
     *
     * <pre>
     * 例子：
     *     String content = "select * from user where id = #para(id)";
     *     SqlPara sqlPara = getSqlPara(content, Kv.of("id", 123));
     *
     * 特别注意：content 参数中不能包含 #sql 指令
     * </pre>
     */
    public SqlPara getSqlPara(String sqlId, String content, Map data) {
        Template template = cache.get(sqlId);
        if (template == null) {
            template = engine.getTemplateByString(content);
            cache.putIfAbsent(sqlId, template);
        }

        SqlPara sqlPara = new SqlPara().setId(sqlId);
        data.put(SQL_PARA_KEY, sqlPara);
        sqlPara.setSql(template.renderToString(data));
        data.remove(SQL_PARA_KEY);    // 避免污染传入的 Map
        return sqlPara;
    }

    /**
     * 通过 String 内容获取 SqlPara 对象
     *
     * <pre>
     * 例子：
     *     String content = "select * from user where id = #para(0)";
     *     SqlPara sqlPara = getSqlPara("user.list", content, 123);
     *
     * 特别注意：content 参数中不能包含 #sql 指令
     * </pre>
     */
    public SqlPara getSqlPara(String sqlId, String content, Object... paras) {
        Template template = cache.get(sqlId);
        if (template == null) {
            template = engine.getTemplateByString(content);
            cache.putIfAbsent(sqlId, template);
        }

        SqlPara sqlPara = new SqlPara().setId(sqlId);
        Map data = new HashMap();
        data.put(SQL_PARA_KEY, sqlPara);
        data.put(PARA_ARRAY_KEY, paras);
        sqlPara.setSql(template.renderToString(data));
        // data 为本方法中创建，不会污染用户数据，无需移除 SQL_PARA_KEY、PARA_ARRAY_KEY
        return sqlPara;
    }

    // ------------------------------------------------------------------------------

    /**
     * 无 sqlId 参数，每次 Enjoy 解析 sql 获取 Template
     */
    public SqlPara getSqlPara(String sql, Map data) {
        Template template = engine.getTemplateByString(sql);
        SqlPara sqlPara = new SqlPara();
        data.put(SQL_PARA_KEY, sqlPara);
        sqlPara.setSql(template.renderToString(data));
        data.remove(SQL_PARA_KEY);    // 避免污染传入的 Map
        return sqlPara;
    }

    /**
     * 无 sqlId 参数，每次 Enjoy 解析 sql 获取 Template
     */
    public SqlPara getSqlPara(String sql, Object... paras) {
        Template template = engine.getTemplateByString(sql);
        SqlPara sqlPara = new SqlPara();
        Map data = new HashMap();
        data.put(SQL_PARA_KEY, sqlPara);
        data.put(PARA_ARRAY_KEY, paras);
        sqlPara.setSql(template.renderToString(data));
        // data 为本方法中创建，不会污染用户数据，无需移除 SQL_PARA_KEY、PARA_ARRAY_KEY
        return sqlPara;
    }
}






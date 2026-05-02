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

import java.util.Arrays;
import java.util.List;

/**
 * Batch 封装批量操作上下文
 */
public class Batch {

    private final DbConfig config;

    // 模式一：使用多个 AifeiRow 子类对象批量插入、更新
    private String table;
    private List<? extends AifeiRow<?>> rowList;

    // 模式二：使用单条 sql 与多条 para 批量插入、更新
    private String sql;
    private List<Object[]> parasList;

    // 模式三：使用多条 sql 批量插入、更新
    private List<String> sqlList;

    private Integer batchSize = null;
    private boolean commitOnBatchSize = false;      // 批量操作数据量达到 batchSize 后是否提交事务
    private boolean getGeneratedKeys = false;       // 批量 "插入" 操作是否获取主键值。只要批量操作中 "存在" 插入操作即可支持
    private boolean putUpdateCountsToRow = false;   // 每个插入/更新操作影响的数据行数是否存放到 row 对象中

    // 回滚事务如果更新数量小于 rowListSize。与 commitOnBatchSize 只能有一个为 true
    // boolean rollbackIfUpdateCountsLessThanRowListSize = false;

    public Batch() {
        config = DbKit.getConfig();     // 非 null 不转调 this(...)
    }

    public Batch(DbConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config can not be null.");
        }
        this.config = config;
    }

    public DbConfig config() {
        return config;
    }

    public String table() {
        return table;
    }

    @SuppressWarnings("unchecked")
    public <T extends AifeiRow<T>> List<T> rowList() {
        return (List<T>) rowList;
    }

    public String sql() {
        return sql;
    }

    public List<Object[]> parasList() {
        return parasList;
    }

    public List<String> sqlList() {
        return sqlList;
    }

    public Batch batchSize(int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("The batchSize must be greater than 0.");
        }
        this.batchSize = batchSize;
        return this;
    }

    public Integer batchSize() {
        return batchSize;
    }

    /**
     * 批量操作数据量达到 batchSize 后是否提交事务
     * 慎用：设置为 true 时，如果发生异常，已提交的数据无法回滚
     */
    public Batch commitOnBatchSize(boolean enable) {
        this.commitOnBatchSize = enable;
        return this;
    }

    public boolean commitOnBatchSize() {
        return commitOnBatchSize;
    }

    /**
     * 是否获取生成的主键值
     */
    public Batch getGeneratedKeys(boolean enable) {
        this.getGeneratedKeys = enable;
        return this;
    }

    public boolean getGeneratedKeys() {
        return getGeneratedKeys;
    }

    /**
     * 每个插入/更新操作影响的数据行数是否存放到 row 对象中
     *
     * 注意：由于批量操作支持每条数据的 table、fields 不相同，所以需要分组进行操作，
     *      分组后的数据次序不同于初始次序，需要需要精确知道当前数据影响的数据行数
     *      需要使用 putUpdateCountsToRow(true) 写数据写入对应的 row 对象中
     */
    public Batch putUpdateCountsToRow(boolean enable) {
        this.putUpdateCountsToRow = enable;
        return this;
    }

    public boolean putUpdateCountsToRow() {
        return putUpdateCountsToRow;
    }

    /**
     * 批量插入。一般用于生成器生成的 AifeiRow 子类对象批量更新
     *
     * 注意：若对象为 Row 则必须为其传入 table 值。生成器生成的 AifeiRow 子类对象不需要
     */
    public BatchResult insert(List<? extends AifeiRow<?>> rowList) {
        this.rowList = rowList;
        return config.batchInsertExecutor.execute(this);
    }

    /**
     * 批量插入。一般用于 Row 对象的批量更新
     *
     * 注意：优先使用 Row 对象中的 table 值，当该值不存在时使用 table 参数值
     */
    public BatchResult insert(String table, List<? extends AifeiRow<?>> rowList) {
        this.table = table;
        this.rowList = rowList;
        return config.batchInsertExecutor.execute(this);
    }

    /**
     * 批量更新。一般用于生成器生成的 AifeiRow 子类对象批量更新
     *
     * 注意：若对象为 Row 则必须为其传入 table 值。生成器生成的 AifeiRow 子类对象不需要
     */
    public BatchResult update(List<? extends AifeiRow<?>> rowList) {
        this.rowList = rowList;
        return config.batchUpdateExecutor.execute(this);
    }

    /**
     * 批量更新。一般用于 Row 对象的批量更新
     *
     * 注意：优先使用 Row 对象中的 table 值，当该值不存在时使用 table 参数值
     */
    public BatchResult update(String table, List<? extends AifeiRow<?>> rowList) {
        this.table = table;
        this.rowList = rowList;
        return config.batchUpdateExecutor.execute(this);
    }

    /**
     * 使用原生 sql + para 进行批量操作，不依赖 Row 对象
     */
    public BatchResult execute(String sql, List<Object[]> parasList) {
        this.sql = sql.trim();
        this.parasList = parasList;
        return config.batchExecutor.execute(this);
    }

    /**
     * 使用原生 sql + para 进行批量操作，不依赖 Row 对象
     */
    public BatchResult execute(String sql, Object[][] parasArray) {
        return execute(sql, Arrays.asList(parasArray));
    }

    /**
     * 使用多条 sql 进行批量操作
     */
    public BatchResult execute(List<String> sqlList) {
        this.sqlList = sqlList;
        return config.batchExecutor.executeSqlList(this);
    }
}




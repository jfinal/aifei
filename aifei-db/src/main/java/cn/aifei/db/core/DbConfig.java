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
import cn.aifei.db.dialect.MysqlDialect;
import cn.aifei.db.dialect.OracleDialect;
import cn.aifei.db.executor.*;
import cn.aifei.db.ext.IdSqlFactories.*;
import cn.aifei.db.factory.*;
import cn.aifei.db.hook.DbHookKit;
import cn.aifei.db.sql.SqlKit;
import cn.aifei.db.transaction.Isolation;
import cn.aifei.db.transaction.Transaction;
import cn.aifei.db.transaction.TransactionExecutor;
import cn.aifei.db.transaction.TransactionKit;
import cn.aifei.enjoy.util.StrUtil;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * DbConfig 封装配置，装配 AifeiDb 组件。
 * 注意：DbConfig 对象在 AifeiDb 中被共享，仅在 AifeiDb 启动前修改，启动之后仅读取。
 *      每个 AifeiDb 的 DbConfig 是独立的，可实现不同数据源独立配置。
 */
public class DbConfig {

    final String id;
    final SqlKit sqlKit;
    final DataSource dataSource;

    Dialect dialect;

    TransactionKit transactionKit = new TransactionKit();
    Isolation transactionIsolation = Isolation.REPEATABLE_READ;

    SqlPrinter sqlPrinter = new SqlPrinter();
    DbHookKit dbHookKit = new DbHookKit();
    TypeConverter typeConverter = new TypeConverter();

    DaoFactory daoFactory = new DaoFactory();
    BatchFactory batchFactory = new BatchFactory();
    IdSqlFactory idSqlFactory = new SqlSelfFactory();   // new MurmurFactory();

    RowFactory rowFactory = new RowFactory();
    DataMapFactory dataMapFactory = new DataMapFactory();
    ChangeSetFactory changeSetFactory = new ChangeSetFactory();

    InsertExecutor insertExecutor = new InsertExecutor();
    DeleteExecutor deleteExecutor = new DeleteExecutor();
    UpdateExecutor updateExecutor = new UpdateExecutor();
    FindExecutor findExecutor = new FindExecutor();
    QueryExecutor queryExecutor = new QueryExecutor();
    PaginateExecutor paginateExecutor = new PaginateExecutor();
    FunExecutor funExecutor = new FunExecutor();
    BatchExecutor batchExecutor = new BatchExecutor();
    BatchInsertExecutor batchInsertExecutor = new BatchInsertExecutor();
    BatchUpdateExecutor batchUpdateExecutor = new BatchUpdateExecutor();
    TransactionExecutor transactionExecutor = new TransactionExecutor();

    int maxResultRows = 0;

    public DbConfig(String id, DataSource dataSource) {
        this(id, dataSource, new MysqlDialect());
    }

    public DbConfig(String id, DataSource dataSource, Dialect dialect) {
        if (StrUtil.isBlank(id)) {
            throw new IllegalArgumentException("id can not be blank.");
        }
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource can not be null.");
        }
        if (dialect == null) {
            throw new IllegalArgumentException("dialect can not be null.");
        }
        this.id = id;
        this.dataSource = dataSource;
        this.dialect = dialect;
        this.sqlKit = new SqlKit(id);
    }

    /**
     * 创建 Dao 对象
     */
    public Dao createDao() {
        return daoFactory.get(this);
    }

    /**
     * 创建 Batch 对象
     */
    public Batch createBatch() {
        return batchFactory.get(this);
    }

    public String getId() {
        return id;
    }

    public SqlKit getSqlKit() {
        return sqlKit;
    }

    public Dialect getDialect() {
        return dialect;
    }

    public DbConfig setDialect(Dialect dialect) {
        if (dialect == null) {
            throw new IllegalArgumentException("dialect can not be null.");
        }

        this.dialect = dialect;

        // Oracle 不支持 REPEATABLE_READ，在配置 Dialect 时默认设置为 READ_COMMITTED
        if (dialect instanceof OracleDialect && transactionIsolation == Isolation.REPEATABLE_READ) {
            transactionIsolation = Isolation.READ_COMMITTED;
        }
        return this;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * 获取 Connection，如果已开启事务，则获取该事务使用的 Connection
     */
    public Connection getConnection() {
        try {
            Transaction<?> transaction = transactionKit.getTransaction();
            return transaction != null ? transaction.getConnection() : dataSource.getConnection();
        } catch (Exception e) {
            throw new AifeiDbException(e);
        }
    }

    /**
     * 关闭 Connection 及其关联的 ResultSet、Statement，如果已开启事务，则不关闭连接
     */
    public void closeConnection(ResultSet resultSet, Statement statement, Connection connection) {
        transactionKit.closeConnection(resultSet, statement, connection);
    }

    public void closeConnection(Connection connection) {
        transactionKit.closeConnection(connection);
    }

    /**
     * 配置是否开启 sql 打印功能，以及是否打印 sql 到日志。
     */
    public DbConfig setPrintSql(boolean printSql, boolean printSqlToLog) {
        sqlPrinter.setPrintSql(printSql, printSqlToLog);
        return this;
    }

    /**
     * 配置是否开启 sql 打印功能，默认值为 false。
     */
    public DbConfig setPrintSql(boolean printSql) {
        sqlPrinter.setPrintSql(printSql);
        return this;
    }

    /**
     * 配置是否打印 sql 到日志，默认值为 false。
     */
    public DbConfig setPrintSqlToLog(boolean printSqlToLog) {
        sqlPrinter.setPrintSqlToLog(printSqlToLog);
        return this;
    }

    /**
     * 配置是否格式化被打印的 sql，默认值为 false。开发环境配置为 true 提升开发体验。
     */
    public DbConfig setFormatSql(boolean formatSql) {
        sqlPrinter.setFormatSql(formatSql);
        return this;
    }

    /**
     * 配置 sql 打印实现类
     */
    public DbConfig setSqlPrinter(SqlPrinter sqlPrinter) {
        Objects.requireNonNull(sqlPrinter, "sqlPrinter can not be null.");
        this.sqlPrinter = sqlPrinter;
        return this;
    }

    public SqlPrinter getSqlPrinter() {
        return sqlPrinter;
    }

    public TransactionKit getTransactionKit() {
        return transactionKit;
    }

    /**
     * 配置 TransactionKit
     */
    public DbConfig setTransactionKit(TransactionKit transactionKit) {
        this.transactionKit = transactionKit;
        return this;
    }

    public Isolation getTransactionIsolation() {
        return transactionIsolation;
    }

    public DbConfig setTransactionIsolation(Isolation isolation) {
        if (isolation == null) {
            throw new IllegalArgumentException("isolation can not be null.");
        }
        this.transactionIsolation = isolation;
        return this;
    }

    public DaoFactory getDaoFactory() {
        return daoFactory;
    }

    public DbConfig setDaoFactory(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
        return this;
    }

    public BatchFactory getBatchFactory() {
        return batchFactory;
    }

    public DbConfig setBatchFactory(BatchFactory batchFactory) {
        this.batchFactory = batchFactory;
        return this;
    }

    public IdSqlFactory getIdSqlFactory() {
        return idSqlFactory;
    }

    public DbConfig setIdSqlFactory(IdSqlFactory idSqlFactory) {
        this.idSqlFactory = idSqlFactory;
        return this;
    }

    public RowFactory getRowFactory() {
        return rowFactory;
    }

    public DbConfig setRowFactory(RowFactory rowFactory) {
        this.rowFactory = rowFactory;
        return this;
    }

    public DataMapFactory getDataMapFactory() {
        return dataMapFactory;
    }

    public DbConfig setDataMapFactory(DataMapFactory dataMapFactory) {
        this.dataMapFactory = dataMapFactory;
        return this;
    }

    public ChangeSetFactory getChangeSetFactory() {
        return changeSetFactory;
    }

    public DbConfig setChangeSetFactory(ChangeSetFactory changeSetFactory) {
        this.changeSetFactory = changeSetFactory;
        return this;
    }

    public InsertExecutor getInsertExecutor() {
        return insertExecutor;
    }

    public DbConfig setInsertExecutor(InsertExecutor insertExecutor) {
        this.insertExecutor = insertExecutor;
        return this;
    }

    public DeleteExecutor getDeleteExecutor() {
        return deleteExecutor;
    }

    public DbConfig setDeleteExecutor(DeleteExecutor deleteExecutor) {
        this.deleteExecutor = deleteExecutor;
        return this;
    }

    public UpdateExecutor getUpdateExecutor() {
        return updateExecutor;
    }

    public DbConfig setUpdateExecutor(UpdateExecutor updateExecutor) {
        this.updateExecutor = updateExecutor;
        return this;
    }

    public FindExecutor getFindExecutor() {
        return findExecutor;
    }

    public DbConfig setFindExecutor(FindExecutor findExecutor) {
        this.findExecutor = findExecutor;
        return this;
    }

    public QueryExecutor getQueryExecutor() {
        return queryExecutor;
    }

    public DbConfig setQueryExecutor(QueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
        return this;
    }

    public PaginateExecutor getPaginateExecutor() {
        return paginateExecutor;
    }

    public DbConfig setPaginateExecutor(PaginateExecutor paginateExecutor) {
        this.paginateExecutor = paginateExecutor;
        return this;
    }

    public FunExecutor getFunExecutor() {
        return funExecutor;
    }

    public DbConfig setFunExecutor(FunExecutor funExecutor) {
        this.funExecutor = funExecutor;
        return this;
    }

    public BatchExecutor getBatchExecutor() {
        return batchExecutor;
    }

    public DbConfig setBatchExecutor(BatchExecutor batchExecutor) {
        this.batchExecutor = batchExecutor;
        return this;
    }

    public BatchInsertExecutor getBatchInsertExecutor() {
        return batchInsertExecutor;
    }

    public DbConfig setBatchInsertExecutor(BatchInsertExecutor batchExecutor) {
        this.batchInsertExecutor = batchExecutor;
        return this;
    }

    public BatchUpdateExecutor getBatchUpdateExecutor() {
        return batchUpdateExecutor;
    }

    public DbConfig setBatchUpdateExecutor(BatchUpdateExecutor batchUpdateExecutor) {
        this.batchUpdateExecutor = batchUpdateExecutor;
        return this;
    }

    public TransactionExecutor getTransactionExecutor() {
        return transactionExecutor;
    }

    public DbConfig setTransactionExecutor(TransactionExecutor transactionExecutor) {
        this.transactionExecutor = transactionExecutor;
        return this;
    }

    /**
     * 配置事务提交之前的回调函数
     */
    public DbConfig setOnBeforeTransactionCommit(BiConsumer<Transaction<?>, Object> onBeforeTransactionCommit) {
        transactionKit.setOnBeforeCommit(onBeforeTransactionCommit);
        return this;
    }

    /**
     * 配置事务抛出异常时的回调函数
     * 注意：当前事务内部通过 tx.onException(...) 配置的回调函数，优先级高于此处的配置
     */
    public DbConfig setOnTransactionException(Function<Exception, ?> onTransactionException) {
        transactionKit.setOnException(onTransactionException);
        return this;
    }

    public TypeConverter getTypeConverter() {
        return typeConverter;
    }

    /**
     * 配置 QueryExecutor 与 AifeiRow 中的 TypeConverter
     * 注意：AifeiRow 中的 TypeConverter 配置是全局性的。QueryExecutor 中的可独立配置
     */
    public DbConfig setTypeConverter(TypeConverter typeConverter) {
        Objects.requireNonNull(typeConverter, "typeConverter can not be null.");
        this.typeConverter = typeConverter;     // 独立配置 QueryExecutor 中的 TypeConverter
        AifeiRow.typeConverter = typeConverter; // 全局配置 AifeiRow 中的 TypeConverter
        return this;
    }

    public DbHookKit getDbHookKit() {
        return dbHookKit;
    }

    public DbConfig setDbHookKit(DbHookKit dbHookKit) {
        this.dbHookKit = dbHookKit;
        return this;
    }

    /**
     * 配置 DbHookKit
     */
    public DbConfig configDbHookKit(Consumer<DbHookKit> hookKit) {
        hookKit.accept(dbHookKit);
        return this;
    }

    public int getMaxResultRows() {
        return maxResultRows;
    }

    /**
     * 配置 find、query 两个系列方法最大结果行数。
     *
     * <pre>
     * 目的：
     * 1: 避免巨量数据被加载，导致应用卡顿甚至 OOM 崩溃。
     * 2: 提高应用并发以及稳定性。
     * </pre>
     */
    public void setMaxResultRows(int maxResultRows) {
        if (maxResultRows < 0) {
            throw new IllegalArgumentException("maxResultRows cannot be negative.");
        }
        this.maxResultRows = maxResultRows;
    }
}




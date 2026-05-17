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

package cn.aifei.db.executor;

import cn.aifei.db.core.*;
import cn.aifei.db.dialect.Dialect;
import cn.aifei.db.transaction.Transaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * BatchExecutor。
 * 1：批量执行：一条 sql + List<Object[]> parasList
 * 2：批量执行：多条无参 List<String> sqlList
 *
 * BatchExecutor 与 BatchInsertExecutor、BatchUpdateExecutor 不同之处：
 * 1：数据无需分组。（因为：要么所有参数共享同一条 sql，要么各 sql 独立且无需共享参数）
 * 2：指定 sql 而非生成 sql
 * 3：不使用 Row 承载数据，而是使用 Object[] 承载数据，或者纯 sql 无数据参数
 */
public class BatchExecutor {

    /**
     * 使用原生 sql + para 进行批量操作
     */
    public BatchResult execute(Batch batch) {
        if (batch.parasList() != null && batch.parasList().size() > 0) {
            return batch.config().createDao().transaction(tx -> batchExecute(tx, batch));
        } else {
            return new BatchResult();
        }
    }

    private BatchResult batchExecute(Transaction<BatchResult> transaction, Batch batch) {
        BatchResult batchResult = new BatchResult();
        DbConfig config = batch.config();
        SqlPrinter sqlPrinter = config.getSqlPrinter();

        Dialect dialect = config.getDialect();

        PreparedStatement preparedStatement = null;
        SqlPara sqlPara = null;
        try {
            Connection connection = transaction.getConnection();

            if (batch.getGeneratedKeys()) {
                // 注意：不支持 oracle、postgresql
                preparedStatement = connection.prepareStatement(batch.sql(), Statement.RETURN_GENERATED_KEYS);
            } else {
                preparedStatement = connection.prepareStatement(batch.sql());
            }

            int count = 0;
            int parasListSize = batch.parasList().size();
            int batchSize = batch.batchSize() != null ? batch.batchSize() : parasListSize;
            for (Object[] paras : batch.parasList()) {
                sqlPara = createSqlPara(batch.sql(), paras);

                dialect.fillStatement(preparedStatement, sqlPara.getPara());
                preparedStatement.addBatch();
                count++;

                // 如不清除，则当参数数量少于问号占位符时将会误用上一轮参数值，导致数据存放错误
                preparedStatement.clearParameters();

                if (count % batchSize == 0 || count >= parasListSize) {
                    sqlPrinter.markExecStart(sqlPara);

                    int[] updateCounts = preparedStatement.executeBatch();
                    if (batch.commitOnBatchSize()) {
                        connection.commit();
                    }
                    batchResult.addUpdateCounts(updateCounts);

                    // 获取生成的主键值
                    if (batch.getGeneratedKeys()) {
                        try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                            while (resultSet.next()) {
                                Object id = resultSet.getObject(1);
                                batchResult.addGeneratedKey(id);    // 主键值存入 BatchResult
                            }
                        }
                    }

                    // 每个 batch 只打印一次
                    sqlPrinter.print(sqlPara);
                }
            }

            return batchResult;

        } catch (Exception e) {
            transaction.rollback();
            printSql(sqlPrinter, sqlPara);
            throw new AifeiDbException(e);
        } finally {
            config.closeConnection(null, preparedStatement, null);
        }
    }

    private SqlPara createSqlPara(String sql, Object[] paras) {
        List<Object> paraList = new ArrayList<>(paras.length);
        for (Object para : paras) {
            paraList.add(para);
        }
        return new SqlPara(sql, paraList);
    }

    // ----------------------------------------------------------------------------

    /**
     * 批量执行多条 sql
     */
    public BatchResult executeSqlList(Batch batch) {
        if (batch.sqlList() != null && batch.sqlList().size() > 0) {
            return batch.config().createDao().transaction(tx -> batchExecuteSqlList(tx, batch));
        } else {
            return new BatchResult();
        }
    }

    private BatchResult batchExecuteSqlList(Transaction<BatchResult> transaction, Batch batch) {
        BatchResult batchResult = new BatchResult();
        DbConfig config = batch.config();
        SqlPrinter sqlPrinter = config.getSqlPrinter();

        Statement statement = null;
        SqlPara sqlPara = null;
        try {
            Connection connection = transaction.getConnection();
            statement = connection.createStatement();

            int count = 0;
            int sqlListSize = batch.sqlList().size();
            int batchSize = batch.batchSize() != null ? batch.batchSize() : sqlListSize;
            List<Object> emptyPara = Collections.emptyList();
            for (String sql : batch.sqlList()) {
                if (sqlPrinter.isPrintSql()) {
                    sqlPara = new SqlPara(sql, emptyPara);
                }

                statement.addBatch(sql);
                count++;

                if (count % batchSize == 0 || count >= sqlListSize) {
                    sqlPrinter.markExecStart(sqlPara);

                    int[] updateCounts = statement.executeBatch();
                    if (batch.commitOnBatchSize()) {
                        connection.commit();
                    }
                    batchResult.addUpdateCounts(updateCounts);

                    // 获取生成的主键值
                    if (batch.getGeneratedKeys()) {
                        try (ResultSet resultSet = statement.getGeneratedKeys()) {
                            while (resultSet.next()) {
                                Object id = resultSet.getObject(1);
                                batchResult.addGeneratedKey(id);    // 主键值存入 BatchResult
                            }
                        }
                    }

                    // 每个 batch 只打印一次
                    sqlPrinter.print(sqlPara);
                }
            }

            return batchResult;

        } catch (Exception e) {
            transaction.rollback();
            printSql(sqlPrinter, sqlPara);
            throw new AifeiDbException(e);
        } finally {
            config.closeConnection(null, statement, null);
        }
    }

    // 避免打印 SQL 时抛出的异常覆盖原始异常
    private void printSql(SqlPrinter sqlPrinter, SqlPara sqlPara) {
        if (sqlPrinter.isPrintSql() && sqlPara != null) {
            try {
                sqlPrinter.print(sqlPara);
            } catch (Exception ignored) {

            }
        }
    }
}


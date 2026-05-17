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
import java.util.List;
import java.util.Map;

/**
 * BatchUpdateExecutor 批量更新执行器
 */
public class BatchUpdateExecutor {

    /**
     * 支持各个 Row 对象中的 table、field 不同时的批量操作：
     *  1：使用 table 与 fields 对数据进行分组
     *  2：对每一组数据进行批量操作
     *  3：必须在事务中执行批量操作，并且在发生异常时回滚事务，因为批量操作本身不具备原子性
     *  4：批量更新考虑了 Set<String> change，被更新数据必须使用 set 而非 put 存入
     */
    public <T extends AifeiRow<T>> BatchResult execute(Batch batch) {
        if (batch.rowList() != null && batch.rowList().size() > 0) {
            Map<String, BatchGroup<T>> batchGroupMap = BatchInsertExecutor.groupByTableAndFields(batch, false);
            return batch.config().createDao().transaction(tx -> batchUpdate(tx, batch, batchGroupMap));
        } else {
            return new BatchResult();
        }
    }

    /**
     * 在事务中对每一组数据进行批量操作
     */
    private <T extends AifeiRow<T>> BatchResult batchUpdate(Transaction<BatchResult> transaction, Batch batch, Map<String, BatchGroup<T>> batchGroupMap) {
        BatchResult batchResult = new BatchResult();
        DbConfig config = batch.config();
        SqlPrinter sqlPrinter = config.getSqlPrinter();

        Dialect dialect = config.getDialect();

        PreparedStatement preparedStatement = null;
        SqlPara sqlPara = null;
        try {
            Connection connection = transaction.getConnection();
            for (Map.Entry<String, BatchGroup<T>> entry : batchGroupMap.entrySet()) {
                BatchGroup<T> batchGroup = entry.getValue();
                List<T> rowList = batchGroup.getRowList();
                sqlPara = dialect.update(rowList.get(0));
                preparedStatement = connection.prepareStatement(sqlPara.getSql());

                int batchBegin = 0;
                int count = 0;
                int rowListSize = rowList.size();
                int batchSize = batch.batchSize() != null ? batch.batchSize() : rowListSize;
                for (int i = 0; i < rowListSize; i++) {
                    sqlPara = dialect.update(rowList.get(i));

                    dialect.fillStatement(preparedStatement, sqlPara.getPara());
                    preparedStatement.addBatch();
                    count++;

                    if (count % batchSize == 0 || count >= rowListSize) {
                        sqlPrinter.markExecStart(sqlPara);

                        int[] updateCounts = preparedStatement.executeBatch();
                        if (batch.commitOnBatchSize()) {
                            connection.commit();
                        }
                        batchResult.addUpdateCounts(updateCounts);

                        // 每个插入/更新操作影响的数据行数存放到 row 对象中
                        if (batch.putUpdateCountsToRow()) {
                            for (int j = 0; j < updateCounts.length; j++) {
                                rowList.get(batchBegin + j).put("updateCounts", updateCounts[j]);
                            }
                        }

                        batchBegin = i + 1;

                        // 每个 batch 只打印一次
                        sqlPrinter.print(sqlPara);
                    }
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



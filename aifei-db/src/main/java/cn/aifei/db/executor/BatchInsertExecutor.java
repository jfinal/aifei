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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BatchUpdateExecutor 批量插入执行器
 */
public class BatchInsertExecutor {

    public <T extends AifeiRow<T>> BatchResult execute(Batch batch) {
        if (batch.rowList() != null && batch.rowList().size() > 0) {
            Map<String, BatchGroup<T>> batchGroupMap = groupByTableAndFields(batch, true);
            return batch.config().createDao().transaction(tx -> batchInsert(tx, batch, batchGroupMap));
        } else {
            return new BatchResult();
        }
    }

    /**
     * 使用 table 与 fields 对数据进行分组
     */
    static <T extends AifeiRow<T>> Map<String, BatchGroup<T>> groupByTableAndFields(Batch batch, boolean isInsertOperation) {
        Map<String, BatchGroup<T>> ret = new LinkedHashMap<>();
        List<T> rowList = batch.rowList();
        String table = batch.table();
        for (T row : rowList) {
            if (row.table() == null) {  // 优先使用 row 对象 table 值，该值为 null 时才干预
                if (table != null) {
                    row.table(table);
                } else {
                    throw new RuntimeException("table can not be null of row : " + row);
                }
            }

            // 更新操作需按 Row 中的 Set<String> change 中字段分组
            Set<String> fieldSet = isInsertOperation ? row.data().keySet() : Cpc.getChange(row);
            String tableAndFields = row.table() + "#" + String.join(",", fieldSet);
            BatchGroup<T> batchGroup = ret.get(tableAndFields);
            if (batchGroup == null) {
                batchGroup = new BatchGroup<>(tableAndFields);
                ret.put(tableAndFields, batchGroup);
            }
            batchGroup.add(row);
        }
        return ret;
    }

    /**
     * 在事务中对每一组数据进行批量操作
     */
    private <T extends AifeiRow<T>> BatchResult batchInsert(Transaction<BatchResult> transaction, Batch batch, Map<String, BatchGroup<T>> batchGroupMap) {
        BatchResult batchResult = new BatchResult();
        DbConfig config = batch.config();
        Dialect dialect = config.getDialect();

        PreparedStatement preparedStatement = null;
        try {
            Connection connection = transaction.getConnection();
            for (Map.Entry<String, BatchGroup<T>> entry : batchGroupMap.entrySet()) {
                BatchGroup<T> batchGroup = entry.getValue();
                List<T> rowList = batchGroup.getRowList();
                SqlPara sqlPara = dialect.insert(rowList.get(0));

                if (batch.getGeneratedKeys()) {
                    preparedStatement = dialect.prepareStatementForReturnGeneratedKeys(connection, sqlPara.getSql(), rowList.get(0).primaryKey());
                } else {
                    preparedStatement = connection.prepareStatement(sqlPara.getSql());
                }

                int batchBegin = 0;
                int count = 0;
                int rowListSize = rowList.size();
                int batchSize = batch.batchSize() != null ? batch.batchSize() : rowListSize;
                for (int i = 0; i < rowListSize; i++) {
                    sqlPara = dialect.insert(rowList.get(i));
                    config.getSqlPrinter().print(sqlPara);

                    dialect.fillStatement(preparedStatement, sqlPara.getPara());
                    preparedStatement.addBatch();
                    count++;

                    if (count % batchSize == 0 || count >= rowListSize) {
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

                        // 获取生成的主键值
                        if (batch.getGeneratedKeys()) {
                            try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                                for (int j = 0; j < updateCounts.length; j++) {
                                    T row = rowList.get(batchBegin + j);
                                    dialect.getGeneratedKeys(resultSet, row);

                                    Object id = row.getObject(row.primaryKey()[0]);
                                    batchResult.addGeneratedKey(id);    // 主键值存一份到 BatchResult
                                }
                            }
                        }

                        batchBegin = i + 1;
                    }
                }
            }
            return batchResult;

        } catch (Exception e) {
            transaction.rollback();
            throw new AifeiDbException(e);
        } finally {
            config.closeConnection(null, preparedStatement, null);
        }
    }
}





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
import cn.aifei.db.hook.InsertHook;
import java.sql.*;

/**
 * InsertExecutor 插入执行器
 */
public class InsertExecutor {

    /**
     * insert into 允许没有主键，不必检查主键与主键值
     */
    public <T extends AifeiRow<T>> T execute(AifeiDao<?, T> dao, T row) {
        DbConfig config = dao.config();
        Dialect dialect = config.getDialect();
        SqlPara sqlPara = dialect.insert(row);
        dao.sqlPara(sqlPara);                       // 供 hook 中取用

        InsertHook insertHook = config.getDbHookKit().getInsertHook();
        Object toAfterRowInsert = insertHook.beforeRowInsert(dao, row);

        sqlPara = dao.sqlPara();                    // 取出 hook 中可能修改过的 sqlPara 供后续使用
        config.getSqlPrinter().print(sqlPara);      // 打印 sql 始终放在 hook 的 before 方法之前

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = config.getConnection();    // 确保每个 Executor 中只有一处 getConnection
            preparedStatement = dialect.prepareStatementForReturnGeneratedKeys(connection, sqlPara.getSql(), row.primaryKey());
            dialect.fillStatement(preparedStatement, sqlPara.getPara());
            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 1) {
                resultSet = preparedStatement.getGeneratedKeys();
                dialect.getGeneratedKeys(resultSet, row);
                insertHook.afterRowInsert(dao, row, toAfterRowInsert);  // 调用钩子函数
                row.clearChange();                                      // 插入之后必须清空 change
                return row;
            } else {                                                    // 不应该发生
                throw new AifeiDbException(
                        String.format("Insert error: expected exactly ONE affected row, but got %d.", affectedRows)
                );
            }

        } catch (Exception e) {
            throw new AifeiDbException(e);
        } finally {
            config.closeConnection(resultSet, preparedStatement, connection);
        }
    }
}




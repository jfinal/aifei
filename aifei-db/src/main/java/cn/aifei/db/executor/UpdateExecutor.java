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
import cn.aifei.db.hook.UpdateHook;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * UpdateExecutor 更新执行器
 */
public class UpdateExecutor {

    /**
     * 通过 SqlPara 更新
     */
    public int execute(AifeiDao<?, ?> dao) {
        DbConfig config = dao.config();
        SqlPrinter sqlPrinter = config.getSqlPrinter();

        UpdateHook updateHook = config.getDbHookKit().getUpdateHook();
        Object toAfterSqlUpdate = updateHook.beforeSqlUpdate(dao);

        SqlPara sqlPara = dao.sqlPara();
        sqlPrinter.markExecStart(sqlPara);

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = config.getConnection();    // 确保每个 Executor 中只有一处 getConnection
            preparedStatement = connection.prepareStatement(sqlPara.getSql());
            config.getDialect().fillStatement(preparedStatement, sqlPara.getPara());
            int ret = preparedStatement.executeUpdate();
            updateHook.afterSqlUpdate(dao, ret, toAfterSqlUpdate);
            return ret;
        } catch (Exception e) {
            throw new AifeiDbException(e);
        } finally {
            config.closeConnection(null, preparedStatement, connection);
            sqlPrinter.print(sqlPara);
        }
    }

    /**
     * 通过 Row 更新
     */
    public <T extends AifeiRow<T>> boolean update(AifeiDao<?, T> dao, T row) {
        // 没有数据需要更新
        if (Cpc.getChange(row).isEmpty()) {
            return false;
        }

        // 主键值为 null 抛出异常，否则没有数据被更新，业务层代码存在安全隐患
        String[] primaryKeys = row.primaryKey();
        for (String pk : primaryKeys) {
            if (row.get(pk) == null) {
                throw new AifeiDbException("Update operation cannot proceed without primary key value: \"" + pk + "\" can not be null.");
            }
        }

        DbConfig config = dao.config();
        SqlPara sqlPara = config.getDialect().update(row);
        dao.sqlPara(sqlPara);

        UpdateHook updateHook = config.getDbHookKit().getUpdateHook();
        Object toAfterRowUpdate = updateHook.beforeRowUpdate(dao, row);

        // 参数个数小于等于主键数量的情况表明只有主键，无需更新
        if (sqlPara.getPara().size() <= primaryKeys.length) {
            return false;
        }

        int affectedRows = execute(dao);
        if (affectedRows == 1) {
            updateHook.afterRowUpdate(dao, row, toAfterRowUpdate);
            row.clearChange();          // 必须放 after 之后，否则钩子函数中无法获取到 change 数据
            return true;
        } else if (affectedRows == 0) { // 与 insert into 不同，数据库层面 update 失败并不会抛出异常，返回 false 即可
            return false;
        } else {                        // Row 对象与数据库记录一一对应，row 的更新只能更新一条数据库记录
            throw new AifeiDbException("The number of rows updated by the primary key cannot be greater than 1.");
        }
    }
}



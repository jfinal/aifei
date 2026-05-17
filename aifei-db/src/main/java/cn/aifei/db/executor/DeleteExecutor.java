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
import cn.aifei.db.hook.DeleteHook;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;

/**
 * DeleteExecutor 删除执行器
 */
public class DeleteExecutor {

    /**
     * 通过 SqlPara 删除
     */
    public int execute(AifeiDao<?, ?> dao) {
        DbConfig config = dao.config();
        SqlPrinter sqlPrinter = config.getSqlPrinter();

        DeleteHook deleteHook = config.getDbHookKit().getDeleteHook();
        Object toAfterSqlDelete = deleteHook.beforeSqlDelete(dao);

        SqlPara sqlPara = dao.sqlPara();
        sqlPrinter.markExecStart(sqlPara);

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = config.getConnection();    // 确保每个 Executor 中只有一处 getConnection
            preparedStatement = connection.prepareStatement(sqlPara.getSql());
            config.getDialect().fillStatement(preparedStatement, sqlPara.getPara());
            int ret = preparedStatement.executeUpdate();
            deleteHook.afterSqlDelete(dao, ret, toAfterSqlDelete);
            return ret;
        } catch (Exception e) {
            throw new AifeiDbException(e);
        } finally {
            config.closeConnection(null, preparedStatement, connection);
            sqlPrinter.print(sqlPara);
        }
    }

    /**
     * 通过 Row 删除
     */
    public <T extends AifeiRow<T>> boolean delete(AifeiDao<?, T> dao, T row) {
        DbConfig config = dao.config();
        SqlPara sqlPara = config.getDialect().delete(row);
        dao.sqlPara(sqlPara);

        DeleteHook deleteHook = config.getDbHookKit().getDeleteHook();
        Object toAfterRowDelete = deleteHook.beforeRowDelete(dao, row);

        int affectedRows = execute(dao);
        if (affectedRows == 1) {
            deleteHook.afterRowDelete(dao, row, toAfterRowDelete);
            row.clearChange();
            return true;
        } else if (affectedRows == 0) { // 与 insert into 不同，数据库层面 delete from 失败并不会抛出异常，返回 false 即可
            return false;
        } else {                        // Row 对象与数据库记录一一对应，row 的删除只能删除一条数据库记录
            throw new AifeiDbException("The number of rows deleted by the primary key cannot be greater than 1.");
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends AifeiRow<T>> boolean deleteById(AifeiDao<?, T> dao, String table, String primaryKey, Object idValue) {
        // Row row = new Row().table(table, primaryKey).put(primaryKey, idValue);
        Row row = new Row().table(table, primaryKey).id(idValue);   // 防止 primaryKey 前后有空白字符
        return delete(dao, (T) row);
    }

    @SuppressWarnings("unchecked")
    public <T extends AifeiRow<T>> boolean deleteByCompositeId(AifeiDao<?, T> dao, String table, String[] primaryKeys, Object[] idValues) {
        // 以下两行验证了 table、primaryKeys 参数的正确性
        Row row = new Row().table(table);
        row.primaryKey(primaryKeys);

        if (idValues == null || idValues.length == 0) {
            throw new IllegalArgumentException("idValues can not be null.");
        }
        if (primaryKeys.length != idValues.length) {
            throw new IllegalArgumentException("The number of primary keys and their values must be equal.");
        }

        // 此处的 primaryKeys 已在 row.primaryKey(...) 中做过 trim() 操作
        for (int i = 0; i < primaryKeys.length; i++) {
            row.put(primaryKeys[i], idValues[i]);
        }

        return delete(dao, (T) row);
    }

    // 本方法已拆分为 deleteById 与 deleteByCompositeId，并转调 delete(dao, row) 实现功能，使得 DeleteHook RowHook 可被应用
    // public <T extends AifeiRow<T>> boolean deleteById(AifeiDao<?, T> dao, String table, String[] primaryKeys, Object[] idValues) {
    //     if (table == null) {
    //         throw new IllegalArgumentException("table can not be null.");
    //     }
    //     if (primaryKeys == null || primaryKeys.length == 0) {
    //         throw new IllegalArgumentException("primaryKeys can not be null.");
    //     }
    //     if (idValues == null || idValues.length == 0) {
    //         throw new IllegalArgumentException("idValues can not be null.");
    //     }
    //     if (primaryKeys.length != idValues.length) {
    //         throw new IllegalArgumentException("The number of primary keys and their values must be equal.");
    //     }
    //
    //     SqlPara sqlPara = dao.config().getDialect().deleteById(table.trim(), primaryKeys, idValues);
    //     dao.sqlPara(sqlPara);
    //     int affectedRows = execute(dao);
    //     if (affectedRows == 1) {
    //         return true;
    //     } else if (affectedRows == 0) { // 与 insert into 不同，数据库层面 delete from 失败并不会抛出异常，返回 false 即可
    //         return false;
    //     } else {                        // Row 对象与数据库记录一一对应，row 的删除只能删除一条数据库记录
    //         throw new AifeiDbException("The number of rows deleted by the primary key cannot be greater than 1");
    //     }
    // }

    /**
     * 支持 where field in (?, ?, ....?) 删除
     */
    public int deleteIn(AifeiDao<?, ?> dao, String table, String field, Collection<?> fieldValues) {
        if (fieldValues == null || fieldValues.isEmpty()) {
            return 0;
        }
        if (table == null) {
            throw new IllegalArgumentException("table can not be null.");
        }
        if (field == null) {
            throw new IllegalArgumentException("field can not be null.");
        }

        SqlPara sqlPara = dao.config().getDialect().deleteIn(table.trim(), field.trim(), fieldValues);
        dao.sqlPara(sqlPara);
        return execute(dao);
    }

    /**
     * deleteBy 支持用法:
     *  1：deleteBy("name", "james);
     *  2：deleteBy("id > ? and age = ?", 5, 18);
     *  3：deleteBy("age = ?", 18);
     *  4：deleteBy("age = 18");
     *
     * 示例：
     *   User.deleteBy("age >= ? and age <= ?", 18, 25);
     */
    public int deleteBy(AifeiDao<?, ?> dao, String table, String whereOrField, Object[] paraArray) {
        if (table == null) {
            throw new IllegalArgumentException("table can not be null.");
        }
        // delete 删除必须带 where 条件
        if (whereOrField == null || whereOrField.trim().isEmpty()) {
            throw new IllegalArgumentException("DANGER DELETE SQL: the where condition can not be blank!");
        }

        SqlPara sqlPara = dao.config().getDialect().deleteBy(table.trim(), whereOrField, paraArray);
        dao.sqlPara(sqlPara);
        return execute(dao);
    }
}



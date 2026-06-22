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
import cn.aifei.db.hook.FindHook;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * FindExecutor 查询执行器
 */
public class FindExecutor {

    public <T extends AifeiRow<T>> List<T> execute(AifeiDao<?, T> dao, Function<T, Boolean> forEachFun) {
        DbConfig config = dao.config();
        SqlPrinter sqlPrinter = config.getSqlPrinter();

        FindHook findHook = config.getDbHookKit().getFindHook();
        Object toAfterFind = findHook.beforeFind(dao);

        SqlPara sqlPara = dao.sqlPara();
        sqlPrinter.startTiming(sqlPara);

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = config.getConnection();    // 确保每个 Executor 中只有一处 getConnection
            preparedStatement = connection.prepareStatement(sqlPara.getSql());
            config.getDialect().fillStatement(preparedStatement, sqlPara.getPara());

            int maxResultRows = config.getMaxResultRows();
            if (maxResultRows > 0) {
                preparedStatement.setMaxRows(maxResultRows);
            }

            resultSet = preparedStatement.executeQuery();
            List<T> ret = config.getRowFactory().get(dao, resultSet, forEachFun);
            findHook.afterFind(dao, ret, toAfterFind);
            return ret;
        } catch (Exception e) {
            throw new AifeiDbException(e);
        } finally {
            config.closeConnection(resultSet, preparedStatement, connection);
            sqlPrinter.print(sqlPara);
        }
    }

    public boolean findExists(AifeiDao<?, ?> dao) {
        boolean[] existence = {false};
        execute(dao, row -> {
            existence[0] = true;
            return false;
        });
        return existence[0];
    }

    @SuppressWarnings({"unchecked"})
    public <T extends AifeiRow<T>> T findFirst(AifeiDao<?, T> dao) {
        Object[] ret = {null};
        execute(dao, row -> {
            ret[0] = row;
            return false;
        });
        return (T) ret[0];
    }

    public <T extends AifeiRow<T>> T findOne(AifeiDao<?, T> dao, boolean allowNull, Function<Integer, String> exceptionMessageFun) {
        List<T> ret = execute(dao, null);
        int size = ret.size();
        if (size == 1) {
            return ret.get(0);
        }
        if (size == 0 && allowNull) {
            return null;
        }

        throw exceptionMessageFun != null ?
                new AifeiDbException(exceptionMessageFun.apply(size)) :
                new AifeiDbException("Expected exactly one result, but found " + size + ". Consider using `findFirst` instead.");
    }

    // public <T extends AifeiRow<T>> T findOne(AifeiDao<?, T> dao, boolean allowNull, String exceptionMessage) {
    //     return findOne(dao, allowNull, size -> exceptionMessage);
    // }

    public <T extends AifeiRow<T>> T findById(AifeiDao<?, T> dao, String table, String[] primaryKeys, Object[] idValues) {
        if (table == null) {
            throw new IllegalArgumentException("table can not be null.");
        }
        if (primaryKeys == null || primaryKeys.length == 0) {
            throw new IllegalArgumentException("primaryKeys can not be null.");
        }
        if (idValues == null || idValues.length == 0) {
            throw new IllegalArgumentException("idValues can not be null.");
        }
        if (primaryKeys.length != idValues.length) {
            throw new IllegalArgumentException("The number of primary keys and their values must be equal.");
        }

        SqlPara sqlPara = dao.config().getDialect().findById(table, dao.select(), primaryKeys, idValues);
        dao.sqlPara(sqlPara);
        List<T> rowList = execute(dao, null);
        int size = rowList.size();
        if (size == 1) {
            return rowList.get(0);
        } else if (size == 0) {
            return null;
        } else {
            throw new AifeiDbException("The number of rows find by the primary key cannot be greater than 1");
        }
    }

    /**
     * 支持 where field in (?, ?, ....?) 查询
     */
    public <T extends AifeiRow<T>> List<T> findIn(AifeiDao<?, T> dao, String table, String field, Collection<?> fieldValues) {
        if (fieldValues == null || fieldValues.isEmpty()) {
            return new ArrayList<>(0);
        }
        if (table == null) {
            throw new IllegalArgumentException("table can not be null.");
        }
        if (field == null) {
            throw new IllegalArgumentException("field can not be null.");
        }

        SqlPara sqlPara = dao.config().getDialect().findIn(table.trim(), dao.select(), field.trim(), fieldValues);
        dao.sqlPara(sqlPara);
        return execute(dao, null);
    }

    /**
     * findBy 可让 model 单表查询 sql 省去除了 where 之外的其它部分，提升开发体验
     *
     * <pre>
     * 例子：
     *  Db 用法需传入参数 table
     *     Db.select("*").findBy("user", "name", "james");
     *     Db.select("*").findBy("user", "id > ? and age = ?", 5, 18);
     *
     *  Model 用法可省去参数 table
     *     User.select("*").findBy("age", 18);
     *     User.select("*").findBy("age = 18");
     *     User.select("*").findBy("age = ? order by id desc", 18);
     *     User.select("id, name").findBy("age >= ? and age <= ?", 18, 25);
     * </pre>
     */
    public <T extends AifeiRow<T>> List<T> findBy(AifeiDao<?, T> dao, String table, String where, Object[] paraArray) {
        if (table == null) {
            throw new IllegalArgumentException("table can not be null.");
        }
        if (where == null) {
            throw new IllegalArgumentException("where can not be null.");
        }

        // paraList 允许为空，支持 where 部分为不带问号占位字符的 sql
        SqlPara sqlPara = dao.config().getDialect().findBy(table.trim(), dao.select(), where, paraArray);
        dao.sqlPara(sqlPara);
        return execute(dao, null);
    }

    public <T extends AifeiRow<T>> T findFirstBy(AifeiDao<?, T> dao, String table, String where, Object[] paraArray) {
        List<T> ret = findBy(dao, table, where, paraArray);
        return ret.size() > 0 ? ret.get(0) : null;
    }

    public <T extends AifeiRow<T>> List<T> findAll(AifeiDao<?, T> dao, String table) {
        if (table == null) {
            throw new IllegalArgumentException("table can not be null.");
        }

        SqlPara sqlPara = dao.config().getDialect().findBy(table.trim(), dao.select(), "", DbKit.EMPTY_OBJECT_ARRAY);
        dao.sqlPara(sqlPara);
        return execute(dao, null);
    }
}

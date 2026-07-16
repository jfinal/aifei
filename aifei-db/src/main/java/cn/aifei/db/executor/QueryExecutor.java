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
import cn.aifei.db.hook.QueryHook;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * QueryExecutor 查询执行器
 */
public class QueryExecutor {

    /**
     * query 系方法返回结果不封装为 Row，所以此处返回值泛型必须为 <T> 而不能为 <T extends AifeiRow<T>>
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> List<T> execute(AifeiDao<?, ?> dao, boolean returnFirst) {
        DbConfig config = dao.config();
        SqlPrinter sqlPrinter = config.getSqlPrinter();

        QueryHook queryHook = config.getDbHookKit().getQueryHook();
        Object toAfterQuery = queryHook.beforeQuery(dao);

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
            int columnCount = resultSet.getMetaData().getColumnCount();
            List result = new ArrayList();
            if (columnCount > 1) {
                while (resultSet.next()) {
                    Object[] columnArray = new Object[columnCount];
                    for (int i = 0; i < columnCount; i++) {
                        columnArray[i] = resultSet.getObject(i + 1);
                    }
                    result.add(columnArray);

                    if (returnFirst) {  // 优化：PreparedStatement.executeQuery() 默认懒加载
                        break;
                    }
                }

            } else if(columnCount == 1) {
                while (resultSet.next()) {
                    result.add(resultSet.getObject(1));

                    if (returnFirst) {  // 优化：PreparedStatement.executeQuery() 默认懒加载
                        break;
                    }
                }
            }

            queryHook.afterQuery(dao, result, toAfterQuery);

            return result;

        } catch (Exception e) {
            throw new AifeiDbException(e);
        } finally {
            config.closeConnection(resultSet, preparedStatement, connection);
            sqlPrinter.print(sqlPara);
        }
    }

    /**
     * 使用 query 方法查询，但仅取第一条数据返回
     *
     * 建议使用 findFirst 代替本方法，findFirst 针对返回多条数据的情况进行了优化。
     * 否则建议在 sql 中限定只返回一条数据，例如 mysql 数据库使用 limit 1
     */
    public <T> T queryFirst(AifeiDao<?, ?> dao) {
        List<T> ret = execute(dao, true);
        return ret.size() > 0 ? ret.get(0) : null;
    }

    public <T> T queryOne(AifeiDao<?, ?> dao, boolean allowNull, Function<Integer, String> exceptionMessageFun) {
        List<T> ret = execute(dao, false);
        int size = ret.size();
        if (size == 1) {
            return ret.get(0);
        }
        if (size == 0 && allowNull) {
            return null;
        }

        String msg = allowNull ?
                "Expected one or zero result, but found " + size + ". Consider using `queryFirst` instead." :
                "Expected exactly one result, but found " + size + ". Consider using `queryFirst` instead.";
        throw exceptionMessageFun != null ?
                new AifeiDbException(exceptionMessageFun.apply(size)) :
                new AifeiDbException(msg);
    }

    public <T> T queryField(AifeiDao<?, ?> dao) {
        List<T> ret = execute(dao, true);
        if (ret.size() > 0) {
            T temp = ret.get(0);
            if (temp instanceof Object[]) {
                throw new AifeiDbException("The queryField method allows querying only a single field.");
            }
            return temp;
        }
        return null;
    }

    public String queryStr(AifeiDao<?, ?> dao) {
        Object value = queryField(dao);
        return dao.config().getTypeConverter().toStr(value);
    }

    public Integer queryInt(AifeiDao<?, ?> dao) {
        Object value = queryField(dao);
        return dao.config().getTypeConverter().toInt(value);
    }

    public Long queryLong(AifeiDao<?, ?> dao) {
        Object value = queryField(dao);
        return dao.config().getTypeConverter().toLong(value);
    }

    public BigDecimal queryBigDecimal(AifeiDao<?, ?> dao) {
        Object value = queryField(dao);
        return dao.config().getTypeConverter().toBigDecimal(value);
    }

    public BigInteger queryBigInteger(AifeiDao<?, ?> dao) {
        Object value = queryField(dao);
        return dao.config().getTypeConverter().toBigInteger(value);
    }

    public Double queryDouble(AifeiDao<?, ?> dao) {
        Object value = queryField(dao);
        return dao.config().getTypeConverter().toDouble(value);
    }

    public Float queryFloat(AifeiDao<?, ?> dao) {
        Object value = queryField(dao);
        return dao.config().getTypeConverter().toFloat(value);
    }

    public Number queryNumber(AifeiDao<?, ?> dao) {
        Object value = queryField(dao);
        return dao.config().getTypeConverter().toNumber(value);
    }

    public Boolean queryBoolean(AifeiDao<?, ?> dao) {
        Object value = queryField(dao);
        return dao.config().getTypeConverter().toBoolean(value);
    }

    public byte[] queryBytes(AifeiDao<?, ?> dao) {
        return queryField(dao);
    }

    public Date queryDate(AifeiDao<?, ?> dao) {
        Object value = queryField(dao);
        return dao.config().getTypeConverter().toDate(value);
    }

    public LocalDateTime queryLocalDateTime(AifeiDao<?, ?> dao) {
        Object value = queryField(dao);
        return dao.config().getTypeConverter().toLocalDateTime(value);
    }

    public LocalDate queryLocalDate(AifeiDao<?, ?> dao) {
        Object value = queryField(dao);
        return dao.config().getTypeConverter().toLocalDate(value);
    }

    public java.sql.Timestamp queryTimestamp(AifeiDao<?, ?> dao) {
        Object value = queryField(dao);
        return dao.config().getTypeConverter().toTimestamp(value);
    }

    public java.sql.Time queryTime(AifeiDao<?, ?> dao) {
        return queryField(dao);
    }

    // ----------------------------------------------------------------------

    public long count(AifeiDao<?, ?> dao, String table) {
        SqlPara sqlPara = dao.config().getDialect().countBy(table.trim(), "", DbKit.EMPTY_OBJECT_ARRAY);
        dao.sqlPara(sqlPara);
        return countBy(dao);
    }

    public long countBy(AifeiDao<?, ?> dao, String table, String where, Object[] paraArray) {
        if (table == null) {
            throw new IllegalArgumentException("table can not be null.");
        }
        if (where == null) {
            throw new IllegalArgumentException("where can not be null.");
        }

        // paraList 允许为空，支持 where 部分为不带问号占位字符的 sql
        SqlPara sqlPara = dao.config().getDialect().countBy(table.trim(), where, paraArray);
        dao.sqlPara(sqlPara);
        return countBy(dao);
    }

    // 为了避免在 where 子句之后使用 group by，新增 countBy 而不使用 queryLong。group by 用法需使用 Db.sql(...)
    private long countBy(AifeiDao<?, ?> dao) {
        List<?> ret = execute(dao, false);  // 传入 false 以便检查返回值数量
        if (ret.size() != 1) {
            if (ret.size() > 1) {
                throw new AifeiDbException("The countBy method cannot use a GROUP BY clause.");
            } else {
                throw new AifeiDbException("The count aggregation query always returns a value. Please check if the SQL statement is correct.");
            }
        }

        Object temp = ret.get(0);
        if (temp instanceof Object[]) {
            throw new AifeiDbException("The countBy method allows querying only a single field.");
        }
        return dao.config().getTypeConverter().toLong(temp);
    }

    // 去除 queryBy 方法，对比直接使用 Db.sql(...).query() 并没有节省代码，反而提高了学习成本与认知负载
    // /**
    //  * queryBy 支持用法:
    //  *    queryBy("user", "name", "james");
    //  *    queryBy("user", "id > ? and age = ?", 5, 18);
    //  *    queryBy("user", "age = ? order by id desc", 18);
    //  *    queryBy("user", "age = 18 order by id desc");
    //  */
    // public <T> List<T> queryBy(AifeiDao<?, ?> dao, String table, String where, Object[] paraArray) {
    //     if (table == null) {
    //         throw new IllegalArgumentException("table can not be null.");
    //     }
    //     if (where == null) {
    //         throw new IllegalArgumentException("where can not be null.");
    //     }
    //
    //     // paraList 允许为空，支持 where 部分为不带问号占位字符的 sql
    //     SqlPara sqlPara = dao.config().getDialect().findBy(table.trim(), dao.select(), where, paraArray);
    //     dao.sqlPara(sqlPara);
    //     return execute(dao, false);
    // }

    // public <T> T queryFirstBy(AifeiDao<?, ?> dao, String table, String where, Object[] paraArray) {
    //     List<T> ret = queryBy(dao, table, where, paraArray);
    //     return ret.size() > 0 ? ret.get(0) : null;
    // }

    // public <T> List<T> queryAll(AifeiDao<?, ?> dao, String table) {
    //     if (table == null) {
    //         throw new IllegalArgumentException("table can not be null.");
    //     }
    //
    //     SqlPara sqlPara = dao.config().getDialect().findBy(table.trim(), dao.select(), "", DbKit.NULL_OBJECT_ARRAY);
    //     dao.sqlPara(sqlPara);
    //     return execute(dao);
    // }
}

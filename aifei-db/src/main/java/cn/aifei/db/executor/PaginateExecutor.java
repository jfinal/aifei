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
import cn.aifei.db.hook.PaginateHook;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * PaginateExecutor 分页执行器
 */
public class PaginateExecutor {

    public <T extends AifeiRow<T>> Page<T> execute(AifeiDao<?, T> dao, int pageNum, int pageSize, Boolean hasGroupBy, TotalRows totalRowsFunction) {
        if (pageNum < 1 || pageSize < 1) {
            throw new IllegalArgumentException("pageNum and pageSize must be greater than 0");
        }

        DbConfig config = dao.config();
        Connection connection = null;
        try {
            // queryTotalRows(...) 与 paginate(...) 共用 Connection 实例
            connection = config.getConnection();    // 确保每个 Executor 中只有一处 getConnection

            // 查询 totalRows
            long totalRows;
            if (totalRowsFunction == null) {
                totalRows = queryTotalRows(dao, hasGroupBy, connection);
            } else {
                final Connection conn = connection;
                totalRows = totalRowsFunction.get(dao.sqlPara(), () -> queryTotalRows(dao, hasGroupBy, conn));
            }

            // 查询分页数据
            return paginate(dao, pageNum, pageSize, totalRows, connection);

        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new AifeiDbException(e);
        } finally {
            config.closeConnection(null, null, connection);
        }
    }

    /**
     * totalRows 查询中的 group by 规则：
     * 1：指定 hasGroupBy 为 true，认定为 group by 查询，返回 "分组数量" groupNum
     * 2：分组数量 groupNum > 1，断定为 group by 查询，返回 "分组数量" groupNum
     * 3：正则匹配发现 group by，认定为 group by 查询，返回 "分组数量" groupNum
     * 4：其它情况，认定为非分组查询，返回 "分组值"，未查到数据返回 0
     */
    private long queryTotalRows(AifeiDao<?, ?> dao, Boolean hasGroupBy, Connection connection) {
        DbConfig config = dao.config();
        SqlPrinter sqlPrinter = config.getSqlPrinter();

        Dialect dialect = config.getDialect();
        SqlPara sqlPara = dialect.paginateTotalRows(dao.sqlPara());

        PaginateHook paginateHook = config.getDbHookKit().getPaginateHook();
        Object toAfterQueryTotalRows = paginateHook.beforeQueryTotalRows(dao, hasGroupBy, sqlPara);

        sqlPrinter.startTiming(sqlPara);

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connection.prepareStatement(sqlPara.getSql());
            dialect.fillStatement(preparedStatement, sqlPara.getPara());
            resultSet = preparedStatement.executeQuery();
            List<Long> longList = new ArrayList<>();
            while (resultSet.next()) {
                longList.add(resultSet.getLong(1));
            }

            int groupNum = longList.size();
            // 指定 hasGroupBy 为 true 或者分组数量 groupNum > 1，返回 "分组数量"
            if ((hasGroupBy != null && hasGroupBy) || groupNum > 1) {
                return groupNum;
            }

            // 未指定 hasGroupBy，但正则匹配到 group by，返回 "分组数量"
            if (hasGroupBy == null && dialect.hasGroupBy(sqlPara.getSql())) {
                return groupNum;
            }

            // 其它情况，认定为非分组查询，返回 "分组值"，未查到数据返回 0
            long ret = groupNum > 0 ? longList.get(0) : 0;
            paginateHook.afterQueryTotalRows(dao, ret, toAfterQueryTotalRows);
            return ret;

        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException) e : new AifeiDbException(e);
        } finally {
            config.closeConnection(resultSet, preparedStatement, null);
            sqlPrinter.print(sqlPara);
        }
    }

    private <T extends AifeiRow<T>> Page<T> paginate(AifeiDao<?, T> dao, int pageNum, int pageSize, long totalRows, Connection connection) throws SQLException {
        if (totalRows == 0) {
            return new Page<>(pageNum, pageSize, 0, 0, new ArrayList<>(0));
        }
        int totalPages = (int) (totalRows / pageSize);
        if (totalRows % pageSize != 0) {
            totalPages++;
        }
        if (pageNum > totalPages) {
            return new Page<>(pageNum, pageSize, totalRows, totalPages, new ArrayList<>(0));
        }

        DbConfig config = dao.config();
        SqlPrinter sqlPrinter = config.getSqlPrinter();

        Dialect dialect = config.getDialect();
        SqlPara sqlPara = dialect.paginate(pageNum, pageSize, dao.sqlPara());

        PaginateHook paginateHook = config.getDbHookKit().getPaginateHook();
        Object toAfterPaginate = paginateHook.beforePaginate(dao, pageNum, pageSize, totalRows, sqlPara);

        sqlPrinter.startTiming(sqlPara);

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connection.prepareStatement(sqlPara.getSql());
            dialect.fillStatement(preparedStatement, sqlPara.getPara());

            resultSet = preparedStatement.executeQuery();
            List<T> rows = config.getRowFactory().get(dao, resultSet, null);
            Page<T> ret = new Page<>(pageNum, pageSize, totalRows, totalPages, rows);
            paginateHook.afterPaginate(dao, ret, toAfterPaginate);
            return ret;

        } finally {
            config.closeConnection(resultSet, preparedStatement, null);
            sqlPrinter.print(sqlPara);
        }
    }

    public <T extends AifeiRow<T>> void forEachPage(AifeiDao<?, T> dao, int pageSize, Boolean hasGroupBy, Function<Page<T>, Boolean> fun) {
        forEachPage(dao, 1, Integer.MAX_VALUE, pageSize, hasGroupBy, fun);
    }

    public <T extends AifeiRow<T>> void forEachPage(AifeiDao<?, T> dao, int startPageNum, int endPageNum, int pageSize, Boolean hasGroupBy, Function<Page<T>, Boolean> fun) {
        // 在整个 forEachPage 过程中缓存 totalRows，确保 totalRows 只查一次数据库
        TotalRows totalRows = new TotalRows() {
            private long totalRows = -1;
            @Override public long get(SqlPara sqlPara, TotalRowsQuery totalRowsQuery) {
                if (totalRows == -1) {
                    totalRows = totalRowsQuery.execute();
                }
                return totalRows;
            }
        };

        for (int i = startPageNum; i <= endPageNum; i++) {
            Page<T> page = execute(dao, i, pageSize, hasGroupBy, totalRows);
            if (page.getRows().isEmpty() || !fun.apply(page)) {
                break;
            }
        }
    }
}



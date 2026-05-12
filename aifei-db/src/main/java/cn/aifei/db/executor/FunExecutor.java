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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * FunExecutor 主要应用场景：
 * 1: 调用存储过程
 * 1: 需要使用原生 Connection 对象
 * 3: 需在同一个 Connection 连接之下执行多条 sql（第 4 条可归入本类）
 * 4: 使用临时表进行性能优化。例如通过 CREATE TEMPORARY TABLE 创建临时表，
 *    然后插入核心数据到临时表，最后再基于临时表进行关联查询得到结果
 *
 * 注意：调用方无需关闭 Connection （原则：谁获取谁关闭）
 */
public class FunExecutor {

    /**
     * 提供 getSqlPara、getSqlParaById 方法获取 enjoy sql，提供 getRowList(ResultSet) 方法
     * 将 ResultSet 转换为 Row 对象
     */
    public static class JdbcKit {
        private final AifeiDao<?, ?> dao;

        private JdbcKit(AifeiDao<?, ?> dao) {
            this.dao = dao;
        }

        public SqlPara getSqlPara(String sql, Object... paras) {
            return dao.config().getSqlKit().getSqlPara(sql, paras);
        }

        public SqlPara getSqlPara(String sql, Map<?, ?> data) {
            return dao.config().getSqlKit().getSqlPara(sql, data);
        }

        public SqlPara getSqlParaById(String sqlId, Object... paras) {
            return dao.config().getSqlKit().getSqlParaById(sqlId, paras);
        }

        public SqlPara getSqlParaById(String sqlId, Map<?, ?> data) {
            return dao.config().getSqlKit().getSqlParaById(sqlId, data);
        }

        public void fillStatement(PreparedStatement pst, List<?> paras) throws SQLException {
            dao.config().getDialect().fillStatement(pst, paras);
        }

        @SuppressWarnings("unchecked")
        public List<Row> getRowList(ResultSet resultSet) throws SQLException {
            return (List<Row>) dao.config().getRowFactory().get(dao, resultSet, null);
        }
    }

    /**
     * 本接口为了避免用户在使用 Connection 时的 try catch 代码，否则可直接使用 JDK Function 接口
     */
    @FunctionalInterface
    public interface JdbcFun<T> {
        T call(Connection conn, JdbcKit kit) throws SQLException;
    }

    public <T> T execute(AifeiDao<?, ?> dao, JdbcFun<T> fun) {
        DbConfig config = dao.config();
        Connection connection = null;
        try {
            connection = config.getConnection();
            return fun.call(connection, new JdbcKit(dao));
        } catch (Exception e) {
            throw new AifeiDbException(e);
        } finally {
            config.closeConnection(null, null, connection);
        }
    }
}


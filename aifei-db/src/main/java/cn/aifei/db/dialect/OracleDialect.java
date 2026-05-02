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

package cn.aifei.db.dialect;

import cn.aifei.db.core.AifeiRow;
import cn.aifei.db.core.SqlPara;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OracleDialect
 *
 * // language=Oracle
 */
public class OracleDialect extends Dialect {

    public OracleDialect() {
        defaultPrimaryKey = new String[]{"ID"};
    }

    @Override
    public char quoteLeft() {
        return '"';
    }

    @Override
    public char quoteRight() {
        return '"';
    }

    @Override
    public String queryTableInfo(String tableName) {
        return "SELECT * FROM " + quoteLeft() + tableName.trim() + quoteRight() + " WHERE rownum < 1";
    }

    /**
     * oracle 返回生成主键值的 prepareStatement 方式与其它数据库不同
     */
    @Override
    public PreparedStatement prepareStatementForReturnGeneratedKeys(Connection conn, String sql, String[] primaryKey) throws SQLException {
        return conn.prepareStatement(sql, primaryKey);
    }

    /**
     * 用于获取 Db.insert(tableName, record) 以后自动生成的主键值，可通过覆盖此方法实现更精细的控制
     * 目前只有 PostgreSqlDialect，覆盖过此方法
     */
    @Override
    public void getGeneratedKeys(ResultSet resultSet, AifeiRow<?> row) throws SQLException {
        for (String pKey : row.primaryKey()) {
            // 或条件 || isOracle() 对于 OracleDialect 来说永真，可去除该 if 判断
            // if (row.get(pKey) == null || isOracle()) {
            if (resultSet.next()) {
                // Class<?> fieldType = table.getColumnType(pKey);
                // 注意：此处 fieldType(pKey) 依赖 Row 的子类覆盖该方法并返回正确的字段类型
                Class<?> fieldType = row.fieldType(pKey);
                if (fieldType == null) {
                    row.put(pKey, resultSet.getObject(1));	// It returns Long for int colType for mysql
                }
                else {	// 支持没有主键的用法，有人将 model 改造成了支持无主键:济南-费小哥
                    if (fieldType == Integer.class || fieldType == int.class) {
                        row.put(pKey, resultSet.getInt(1));
                    } else if (fieldType == Long.class || fieldType == long.class) {
                        row.put(pKey, resultSet.getLong(1));
                    } else if (fieldType == BigInteger.class) {
                        handleGeneratedBigIntegerKey(row, pKey, resultSet.getObject(1));
                    } else {
                        row.put(pKey, resultSet.getObject(1));	// It returns Long for int colType for mysql
                    }
                }
            }
            // }
        }
    }

    /**
     * 增
     * 通过 ".nextval" 支持 oracle 自增主键
     */
    @Override
    public SqlPara insert(AifeiRow<?> row) {
        StringBuilder sql = new StringBuilder(80 + row.size() * 20)
                .append("INSERT INTO ")
                .append(quoteLeft()).append(row.table()).append(quoteRight())
                .append("(");
        StringBuilder valueSql = new StringBuilder(row.size() * 20).append(") VALUES(");
        List<Object> paraList = new ArrayList<>(row.size());

        String[] primaryKeys = row.primaryKey();
        for (Map.Entry<String, Object> e : row.data().entrySet()) {
            String field = e.getKey();
            if (row.columnDefined(field)) {
                if (paraList.size() > 0) {
                    sql.append(", ");
                    valueSql.append(", ");
                }
                sql.append(quoteLeft()).append(field).append(quoteRight());

                // 值为 ".nextval" 时处理成自增主键
                Object value = e.getValue();
                if (value instanceof String && isPrimaryKey(field, primaryKeys) && ((String)value).endsWith(".nextval")) {
                    valueSql.append(value);
                } else {
                    valueSql.append('?');
                    paraList.add(value);
                }
            }
        }
        sql.append(valueSql).append(')');

        return new SqlPara(sql.toString(), paraList);
    }

    @Override
    public SqlPara paginate(int pageNum, int pageSize, SqlPara sqlPara) {
        int start = (pageNum - 1) * pageSize;
        int end = pageNum * pageSize;
        String findSql = sqlPara.getSql();
        StringBuilder ret = new StringBuilder(findSql.length() + 150);
        ret.append("SELECT * FROM ( SELECT row_.*, rownum rownum_ FROM (  ");
        ret.append(findSql);
        ret.append(" ) row_ WHERE rownum <= ").append(end).append(") table_alias");
        ret.append(" WHERE table_alias.rownum_ > ").append(start);
        return new SqlPara(ret.toString(), sqlPara.getPara());
    }
}




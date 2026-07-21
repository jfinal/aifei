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

import cn.aifei.db.core.Cpc;
import cn.aifei.db.core.AifeiRow;
import cn.aifei.db.core.SqlPara;
import cn.aifei.db.util.PageSqlUtil;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Dialect
 *
 * <pre>
 * IDEA 支持 String 变量中的 sql 语法高亮注释：
 *   标准SQL：            // language=SQL
 *   MySQL：             // language=MySQL
 *   SQL Server：        // language=TSQL
 *   Oracle：            // language=Oracle
 *   PostgreSQL：        // language=PostgreSQL
 *   SQLite：            // language=SQLite
 * </pre>
 */
public abstract class Dialect {

    // 抽象 -----------------------------------------------------------------------------------

    public abstract char quoteLeft();

    public abstract char quoteRight();

    public abstract SqlPara paginate(int pageNum, int pageSize, SqlPara sqlPara);

    // 实现 -----------------------------------------------------------------------------------

    protected String[] defaultPrimaryKey = new String[]{"id"};

    protected Pattern orderByPattern = Pattern.compile(
            "\\border\\s+by\\b\\s+[^;]+?(?=(\\b(limit|offset|fetch|union)\\b|$|;))",
            Pattern.CASE_INSENSITIVE
    );

    protected Pattern groupByPattern = Pattern.compile(
            "\\bgroup\\s+by\\b", Pattern.CASE_INSENSITIVE
    );

    public String[] getDefaultPrimaryKey() {
        return defaultPrimaryKey;
    }

    public void setDefaultPrimaryKey(String... defaultPrimaryKey) {
        this.defaultPrimaryKey = defaultPrimaryKey;
    }

    public Pattern getOrderByPattern() {
        return orderByPattern;
    }

    public void setOrderByPattern(String orderByPattern) {
        this.orderByPattern = Pattern.compile(orderByPattern);
    }

    public Pattern getGroupByPattern() {
        return groupByPattern;
    }

    public void setGroupByPattern(String groupByPattern) {
        this.groupByPattern = Pattern.compile(groupByPattern);
    }

    /**
     * MetaGenerator 查询 TableInfo 所使用的 sql
     */
    public String queryTableInfo(String tableName) {
        return "SELECT * FROM " + quoteLeft() + tableName.trim() + quoteRight() + " WHERE 1 = 2";
    }

    /**
     * 执行 prepareStatement 执行 insert into 语句后返回生成的主键值
     * 用途：用于支持 oracle 获取生成的主键值
     */
    public PreparedStatement prepareStatementForReturnGeneratedKeys(Connection conn, String sql, String[] primaryKey) throws SQLException {
        return conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }

    /**
     * 保守处理 JDBC 日期参数，供不能从普通 java.util.Date
     * 推断 SQL 类型的驱动使用。有明确驱动支持的方言可以覆盖本方法，
     * 将参数直接交给 PreparedStatement.setObject(...)。
     */
    public void fillStatement(PreparedStatement pst, List<?> paras) throws SQLException {
        if (paras != null) {
            for (int i = 0, size = paras.size(); i < size; i++) {
                Object value = paras.get(i);
                if (value instanceof java.util.Date) {
                    if (value.getClass() == java.util.Date.class) {
                        // 普通 Date 表示时间点，按 JDBC TIMESTAMP 绑定。
                        pst.setTimestamp(i + 1, new java.sql.Timestamp(((java.util.Date) value).getTime()));
                    } else if (value instanceof java.sql.Timestamp) {
                        pst.setTimestamp(i + 1, (java.sql.Timestamp) value);
                    } else if (value instanceof java.sql.Date) {
                        pst.setDate(i + 1, (java.sql.Date) value);
                    } else if (value instanceof java.sql.Time) {
                        pst.setTime(i + 1, (java.sql.Time) value);
                    } else {
                        // 其它 java.util.Date 子类也按时间点处理。
                        pst.setTimestamp(i + 1, new java.sql.Timestamp(((java.util.Date) value).getTime()));
                    }
                } else {
                    pst.setObject(i + 1, value);
                }
            }
        }
    }

    /**
     * 用于获取 Db.insert(row) 以后自动生成的主键值，可通过覆盖此方法实现更精细的控制
     * 目前只有 PostgreSqlDialect，覆盖过此方法
     */
    public void getGeneratedKeys(ResultSet resultSet, AifeiRow<?> row) throws SQLException {
        for (String pKey : row.primaryKey()) {
            if (row.get(pKey) == null) {
                if (resultSet.next()) {
                    // Class<?> fieldType = table.getColumnType(pKey);
                    // 注意：此处 fieldType(pKey) 依赖 Row 的子类覆盖该方法并返回正确的字段类型
                    Class<?> fieldType = row.fieldType(pKey);
                    if (fieldType == null) {
                        row.put(pKey, resultSet.getObject(1));    // It returns Long for int colType for mysql
                    } else {    // 支持没有主键的用法，有人将 model 改造成了支持无主键:济南-费小哥
                        if (fieldType == Integer.class || fieldType == int.class) {
                            row.put(pKey, resultSet.getInt(1));
                        } else if (fieldType == Long.class || fieldType == long.class) {
                            row.put(pKey, resultSet.getLong(1));
                        } else if (fieldType == BigInteger.class) {
                            handleGeneratedBigIntegerKey(row, pKey, resultSet.getObject(1));
                        } else {
                            row.put(pKey, resultSet.getObject(1));    // It returns Long for int colType for mysql
                        }
                    }
                }
            }
        }
    }

    /**
     * mysql 数据库的  bigint unsigned 对应的 java 类型为 BigInteger
     * 但是 rs.getObject(1) 返回值为 Long 型，造成 model.insert() 以后
     * model.getId() 时的类型转换异常
     */
    protected void handleGeneratedBigIntegerKey(AifeiRow<?> row, String pKey, Object v) {
        if (v instanceof BigInteger) {
            row.put(pKey, v);
        } else if (v instanceof Number) {
            Number n = (Number) v;
            row.put(pKey, BigInteger.valueOf(n.longValue()));
        } else {
            row.put(pKey, v);
        }
    }

    /**
     * 增
     */
    public SqlPara insert(AifeiRow<?> row) {
        StringBuilder sql = new StringBuilder(80 + row.size() * 20);
        sql.append("INSERT INTO ").append(quoteLeft()).append(row.table()).append(quoteRight()).append("(");
        StringBuilder valueSql = new StringBuilder(row.size() * 20).append(") VALUES(");
        List<Object> paraList = new ArrayList<>(row.size());

        for (Map.Entry<String, Object> e : row.data().entrySet()) {
            String field = e.getKey();
            if (row.columnDefined(field)) {
                if (paraList.size() > 0) {
                    sql.append(',');
                    valueSql.append(',');
                }
                sql.append(quoteLeft()).append(field).append(quoteRight());
                valueSql.append('?');
                paraList.add(e.getValue());
            }
        }
        sql.append(valueSql).append(')');

        return new SqlPara(sql.toString(), paraList);
    }

    public SqlPara delete(AifeiRow<?> row) {
        String table = row.table();
        String[] primaryKeys = row.primaryKey();

        StringBuilder sql = new StringBuilder(50 + table.length() + primaryKeys.length * 20);
        List<Object> paraList = new ArrayList<>(primaryKeys.length);

        sql.append("DELETE FROM ").append(quoteLeft()).append(table).append(quoteRight()).append(" WHERE ");
        for (int i = 0; i < primaryKeys.length; i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(quoteLeft()).append(primaryKeys[i]).append(quoteRight()).append(" = ?");
            paraList.add(row.get(primaryKeys[i]));
        }

        return new SqlPara(sql.toString(), paraList);
    }

    /**
     * deleteBy 支持用法:
     *  1：deleteBy("id > ? and age > ?", 5, 18);
     *  2：deleteBy("age > ?", 18);
     *  3：deleteBy("age", 18);  // 等价于 deleteBy("age = ?", 18);
     *
     * Model 使用示例：
     *   User.deleteBy("age >= ? and age <= ?", 18, 25);
     */
    public SqlPara deleteBy(String table, String whereOrField, Object[] paraArray) {
        StringBuilder sql = new StringBuilder(20 + table.length() + whereOrField.length() + paraArray.length);
        sql.append("DELETE FROM ").append(quoteLeft()).append(table).append(quoteRight());

        // 支持单字段相等条件删除，例如：deleteBy("age", 18)
        if (paraArray.length == 1 && !whereOrField.contains("?")) {
            sql.append(" WHERE ").append(quoteLeft()).append(whereOrField.trim()).append(quoteRight()).append(" = ?");
        } else {                // delete sql 必须传入 where 条件，确保数据安全
            sql.append(" WHERE ").append(whereOrField);
        }

        return new SqlPara(sql.toString(), Arrays.asList(paraArray));
    }

    /**
     * 支持 where field in (?, ?, ....?) 删除
     */
    public SqlPara deleteIn(String table, String field, Collection<?> fieldValues) {
        StringBuilder sql = new StringBuilder(50 + table.length() + field.length() + fieldValues.size());
        List<Object> paraList = new ArrayList<>(fieldValues.size());

        sql.append("DELETE FROM ").append(quoteLeft()).append(table).append(quoteRight());
        sql.append(" WHERE ").append(quoteLeft()).append(field).append(quoteRight()).append(" IN (");

        int i = 0;
        for (Object value : fieldValues) {
            if (i++ > 0) {
                sql.append(',');
            }
            sql.append('?');
            paraList.add(value);
        }
        sql.append(")");

        return new SqlPara(sql.toString(), paraList);
    }

    /**
     * 改
     */
    public SqlPara update(AifeiRow<?> row) {
        StringBuilder sql = new StringBuilder(60 + (row.size() + 3) * 20);
        List<Object> paraList = new ArrayList<>(row.size());

        sql.append("UPDATE ").append(quoteLeft()).append(row.table()).append(quoteRight()).append(" SET ");

        Set<String> changeSet = Cpc.getChange(row);
        String[] primaryKeys = row.primaryKey();
        for (Map.Entry<String, Object> e : row.data().entrySet()) {
            String field = e.getKey();
            if (changeSet.contains(field) && !isPrimaryKey(field, primaryKeys) && row.columnDefined(field)) {
                if (paraList.size() > 0) {
                    sql.append(',');
                }
                sql.append(quoteLeft()).append(field).append(quoteRight()).append(" = ? ");
                paraList.add(e.getValue());
            }
        }

        sql.append(" WHERE ");
        for (int i = 0; i < primaryKeys.length; i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(quoteLeft()).append(primaryKeys[i]).append(quoteRight()).append(" = ?");
            paraList.add(row.get(primaryKeys[i]));
        }

        return new SqlPara(sql.toString(), paraList);
    }

    /**
     * 判断 field 是否为主键
     */
    protected boolean isPrimaryKey(String field, String[] primaryKeys) {
        for (String pk : primaryKeys) {
            if (field.equalsIgnoreCase(pk)) {
                return true;
            }
        }
        return false;
    }

    /**
     * findById
     */
    public SqlPara findById(String table, String selectedFields, String[] primaryKeys, Object[] idValues) {
        StringBuilder sql = new StringBuilder(50 + table.length() + selectedFields.length() + primaryKeys.length * 20);
        List<Object> paraList = new ArrayList<>(primaryKeys.length);

        sql.append("SELECT ").append(selectedFields);
        sql.append(" FROM ").append(quoteLeft()).append(table.trim()).append(quoteRight()).append(" WHERE ");

        for (int i = 0; i < primaryKeys.length; i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(quoteLeft()).append(primaryKeys[i].trim()).append(quoteRight()).append(" = ?");
            paraList.add(idValues[i]);
        }

        return new SqlPara(sql.toString(), paraList);
    }

    /**
     * findBy 支持用法:
     *  1：findBy("name", "james");
     *  2：findBy("id > ? and age = ?", 5, 18);
     *  3：findBy("age = ? order by id desc", 18);
     *  4：findBy("age = 18 order by id desc");
     */
    public SqlPara findBy(String table, String selectedFields, String where, Object[] paraArray) {
        StringBuilder sql = new StringBuilder(30 + table.length() + selectedFields.length() + where.length() + paraArray.length);

        sql.append("SELECT ").append(selectedFields);
        sql.append(" FROM ").append(quoteLeft()).append(table).append(quoteRight());

        // 支持单字段相等条件查询，例如：findBy("name", "james")
        if (paraArray.length == 1 && !where.contains("?")) {
            sql.append(" WHERE ").append(quoteLeft()).append(where.trim()).append(quoteRight()).append(" = ?");
        } else if (where.length() > 0) {   // 支持 findAll(table, selectedFields)
            sql.append(" WHERE ").append(where);
        }

        return new SqlPara(sql.toString(), Arrays.asList(paraArray));
    }

    /**
     * 支持 where field in (?, ?, ....?) 查询
     */
    public SqlPara findIn(String table, String selectedFields, String field, Collection<?> fieldValues) {
        StringBuilder sql = new StringBuilder(50 + table.length() + field.length() + selectedFields.length() + fieldValues.size());
        List<Object> paraList = new ArrayList<>(fieldValues.size());

        sql.append("SELECT ").append(selectedFields);
        sql.append(" FROM ").append(quoteLeft()).append(table).append(quoteRight());
        sql.append(" WHERE ").append(quoteLeft()).append(field).append(quoteRight()).append(" IN (");

        int i = 0;
        for (Object value : fieldValues) {
            if (i++ > 0) {
                sql.append(',');
            }
            sql.append('?');
            paraList.add(value);
        }
        sql.append(")");

        return new SqlPara(sql.toString(), paraList);
    }

    public SqlPara countBy(String table, String where, Object[] paraArray) {
        StringBuilder sql = new StringBuilder(20 + table.length() + where.length() + paraArray.length);
        sql.append("SELECT COUNT(*) FROM ").append(quoteLeft()).append(table).append(quoteRight());

        // 支持单字段相等条件 count，例如：count("age", 18)
        if (paraArray.length == 1 && !where.contains("?")) {
            sql.append(" WHERE ").append(quoteLeft()).append(where.trim()).append(quoteRight()).append(" = ?");
        } else if (where.length() > 0) {   // 支持 User.count()
            sql.append(" WHERE ").append(where);
        }

        return new SqlPara(sql.toString(), Arrays.asList(paraArray));
    }

    /**
     * 移除 sql 中的 order by 子句，用于分页方法查询 totalRows 值
     * 因为除了 mysql 以外多数的数据库不支持 select count(*) 查询使用 order by 子句
     *
     * 注意：个别复杂 order by 子句无法正确清除 order by 的情况，可使用如下方式手动查询出 totalRows 值：
     *         Db.sql(...).paginate(1, 10, (sqlPara, totalRowsQuery) -> {
     *             return Db.sql("select count(*) from user", sqlPara.getPara()).queryLong();
     *         });
     *
     *      以上用法在 paginate 第三个参数中手动查询出 totalRows 值，即可解决 order by 清除不干净的问题
     */
    public String removeOrderBy(String sql) {
        return orderByPattern.matcher(sql).replaceAll("");
    }

    public boolean hasGroupBy(String sql) {
        return groupByPattern.matcher(sql).find();
    }

    /**
     * 分页查询总记录数 totalRows
     */
    public SqlPara paginateTotalRows(SqlPara sqlPara) {
        String[] array = PageSqlUtil.parsePageSql(sqlPara.getSql());    // 分页 sql 拆分
        String sqlWithoutSelect = removeOrderBy(array[1]);              // removeOrderBy(...) 移除 order by 子句
        StringBuilder sql = new StringBuilder(16 + sqlWithoutSelect.length());
        sql.append("SELECT COUNT(*) ").append(sqlWithoutSelect);
        return new SqlPara(sql.toString(), sqlPara.getPara());
    }

    /**
     * 从 ResultSet 读取字段值。
     *
     * <pre>
     * 默认通过 ResultSet.getObject(int) 保留 JDBC 驱动返回的类型，仅对驱动实际
     * 返回的 LOB 对象做物化处理。数据库或 JDBC 驱动有特殊读取规则时，
     * 由具体 Dialect 覆盖本方法，并可将其它类型交回 super.readColumnValue(...)。
     * </pre>
     */
    public Object readColumnValue(ResultSet rs, int columnIndex, int jdbcType) throws SQLException {
        // String、Integer、Long、byte[] 等高频类型走 getObject 快速路径。
        // JDBC Types 常量的数值不是类型分类；这个判断只是性能优化，其余类型仍以 getObject 兜底。
        if (jdbcType < Types.DATE) {
            return rs.getObject(columnIndex);
        }

        Object value = rs.getObject(columnIndex);
        switch (jdbcType) {
            case Types.BLOB:
                return value instanceof Blob ? handleBlob((Blob) value) : value;
            case Types.CLOB:
            case Types.NCLOB:
                return value instanceof Clob ? handleClob((Clob) value) : value;
            default:
                return value;
        }
    }

    protected byte[] handleBlob(Blob blob) throws SQLException {
        if (blob == null) {
            return null;
        }

        long length = blob.length();
        if (length == 0) {
            return new byte[0];
        }
        if (length > Integer.MAX_VALUE) {
            throw new SQLException("Blob is too large to convert to byte[].");
        }

        try (InputStream is = blob.getBinaryStream()) {
            if (is == null) {
                return null;
            }
            byte[] data = new byte[(int) length];
            int offset = 0;
            while (offset < data.length) {
                int read = is.read(data, offset, data.length - offset);
                if (read <= 0) {
                    break;
                }
                offset += read;
            }
            return offset == data.length ? data : Arrays.copyOf(data, offset);
        } catch (IOException e) {
            throw new SQLException("Failed to read Blob data.", e);
        }
    }

    protected String handleClob(Clob clob) throws SQLException {
        if (clob == null) {
            return null;
        }
        long length = clob.length();
        if (length > Integer.MAX_VALUE) {
            throw new SQLException("Clob is too large to convert to String.");
        }
        return clob.getSubString(1, (int) length);
    }
}


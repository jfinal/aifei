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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * H2Database Dialect
 *
 * 注意：使用 h2database 数据库存取 blob 数据时需要参考 jfinal 中 H2RecordBuilder 代码扩展 RowFactory
 */
public class H2Dialect extends Dialect {

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
     * H2 的 byte[] 使用 setBytes()；驱动本身能区分 JDBC 日期子类，
     * 并将普通 java.util.Date 按 TIMESTAMP 处理。
     */
    @Override
    public void fillStatement(PreparedStatement pst, List<?> paras) throws SQLException {
        if (paras != null) {
            for (int i = 0, size = paras.size(); i < size; i++) {
                Object value = paras.get(i);
                if (value instanceof byte[]) {
                    pst.setBytes(i + 1, (byte[]) value);
                } else {
                    pst.setObject(i + 1, value);
                }
            }
        }
    }

    /**
     * 增
     * 通过 ".nextval" 支持自增主键
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



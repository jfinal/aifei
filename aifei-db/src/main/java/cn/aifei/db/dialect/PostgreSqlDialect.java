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
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreSqlDialect
 *
 * // language=PostgreSQL
 */
public class PostgreSqlDialect extends Dialect {

    @Override
    public char quoteLeft() {
        return '"';
    }

    @Override
    public char quoteRight() {
        return '"';
    }

    /**
     * 解决 PostgreSql 获取自增主键时 rs.getObject(1) 总是返回第一个字段的值，而非返回了 id 值
     * issue: https://www.oschina.net/question/2312705_2243354
     *
     * 相对于 Dialect 中的默认实现，仅将 rs.getXxx(1) 改成了 rs.getXxx(pKey)
     */
    @Override
    public void getGeneratedKeys(ResultSet resultSet, AifeiRow<?> row) throws SQLException {
        for (String pKey : row.primaryKey()) {
            if (row.get(pKey) == null) {
                if (resultSet.next()) {
                    // Class<?> fieldType = table.getColumnType(pKey);
                    // 注意：此处 fieldType(pKey) 依赖 Row 的子类覆盖该方法并返回正确的字段类型
                    Class<?> fieldType = row.fieldType(pKey);
                    if (fieldType == null) {
                        row.put(pKey, resultSet.getObject(pKey));
                    }
                    else {
                        if (fieldType == Integer.class || fieldType == int.class) {
                            row.put(pKey, resultSet.getInt(pKey));
                        } else if (fieldType == Long.class || fieldType == long.class) {
                            row.put(pKey, resultSet.getLong(pKey));
                        } else if (fieldType == BigInteger.class) {
                            handleGeneratedBigIntegerKey(row, pKey, resultSet.getObject(pKey));
                        } else {
                            row.put(pKey, resultSet.getObject(pKey));
                        }
                    }
                }
            }
        }
    }

    @Override
    public SqlPara paginate(int pageNum, int pageSize, SqlPara sqlPara) {
        int offset = (pageNum - 1) * pageSize;
        String findSql = sqlPara.getSql();
        StringBuilder ret = new StringBuilder(findSql.length() + 30);
        ret.append(findSql).append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);
        return new SqlPara(ret.toString(), sqlPara.getPara());
    }
}







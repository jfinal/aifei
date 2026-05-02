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

package cn.aifei.db.factory;

import cn.aifei.db.core.*;
import cn.aifei.enjoy.util.InstanceUtil;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 从 ResultSet 在获取数据并放入 Row 或者 Row 的子类
 *
 * <p>
 * 如需针对 mybatis 用户使用习惯，避免 JDBC 将 Byte、Short 转成 Integer，
 * 可将继承 RowFactory 并将 if (types[i] < Types.DATE) 代码块改为如下内容：
 *     if (types[i] < Types.DATE) {
 *         if (types[i] == Types.TINYINT) {
 *             value = BuilderKit.getByte(rs, i);
 *         } else if (types[i] == Types.SMALLINT) {
 *             value = BuilderKit.getShort(rs, i);
 *         } else {
 *             value = rs.getObject(i);
 *         }
 *     }
 *
 * <p>
 * 注：可优先尝试 rs.getByte(i) 与 ResultSet.getShort(i) 是否满足需求，
 *      否则使用 jfinal 源码中的 BuilderKit.java 实现 Byte、Short 值获取
 */
public class RowFactory implements Serializable {

    @SuppressWarnings("unchecked")
    public <T extends AifeiRow<T>> List<T> get(AifeiDao<?, T> dao, ResultSet rs, Function<T, Boolean> forEachFun) throws SQLException {
        List<T> result = new ArrayList<>();

        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();
        String[] labelNames = new String[columnCount + 1];
        int[] types = new int[columnCount + 1];
        buildLabelNamesAndTypes(rsmd, labelNames, types);
        DataMapFactory dataMapFactory = dao.dataMapFactory();

        while (rs.next()) {
            Map<String, Object> data = dataMapFactory.get();
            for (int i = 1; i <= columnCount; i++) {
                Object value;
                if (types[i] < Types.DATE) {
                    value = rs.getObject(i);
                } else {
                    if (types[i] == Types.TIMESTAMP) {
                        value = rs.getTimestamp(i);
                    } else if (types[i] == Types.DATE) {
                        value = rs.getDate(i);
                    } else if (types[i] == Types.CLOB) {
                        value = handleClob(rs.getClob(i));
                    } else if (types[i] == Types.NCLOB) {
                        value = handleClob(rs.getNClob(i));
                    } else if (types[i] == Types.BLOB) {
                        value = handleBlob(rs.getBlob(i));
                    } else {
                        value = rs.getObject(i);
                    }
                }

                data.put(labelNames[i], value);
            }

            AifeiRow<?> row = newRow(dao.rowType()).data(data);
            if (forEachFun == null) {
                result.add((T) row);
            } else {
                if (!forEachFun.apply((T) row)) {
                    break;
                }
            }
        }

        return result;
    }

    protected AifeiRow<?> newRow(Class<? extends AifeiRow<?>> rowType) {
        // 类型为 Row 时避免使用反射
        return rowType == Row.class ? new Row() : InstanceUtil.get(rowType);
    }

    protected void buildLabelNamesAndTypes(ResultSetMetaData rsmd, String[] labelNames, int[] types) throws SQLException {
        for (int i = 1; i < labelNames.length; i++) {
            // 备忘：getColumnLabel 获取 sql as 子句指定的名称而非字段真实名称
            labelNames[i] = rsmd.getColumnLabel(i);
            types[i] = rsmd.getColumnType(i);
        }
    }

    protected byte[] handleBlob(Blob blob) throws SQLException {
        if (blob == null) {
            return null;
        }

        try (InputStream is = blob.getBinaryStream()) {
            if (is == null) {
                return null;
            }
            byte[] data = new byte[(int) blob.length()];     // byte[] data = new byte[is.available()];
            if (data.length == 0) {
                return null;
            }
            is.read(data);
            return data;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String handleClob(Clob clob) throws SQLException {
        return clob == null ? null : clob.getSubString(1, (int) clob.length());
    }
}




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
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 从 ResultSet 在获取数据并放入 Row 或者 Row 的子类
 *
 * <pre>
 * 默认通过 ResultSet.getObject(...) 保留 JDBC 驱动返回的类型，仅对驱动实际返回的 LOB 对象做物化处理。
 *
 * 如需定制字段读取规则，可继承 RowFactory 并覆盖 readValue(...)。
 * </pre>
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
                data.put(labelNames[i], readValue(rs, i, types[i]));
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

    /**
     * 必须以 ResultSet.getObject(int) 作为默认读取方式。
     *
     * <pre>
     * JDBC 规范明确定义：
     * ResultSetMetaData.getColumnClassName(int) 返回调用 ResultSet.getObject(int)
     * 读取该列时所创建对象的类名，实际对象允许是该类的子类。
     *
     * MetaReader 优先使用这个类名生成字段类型，因此这里不能随意改用
     * getDate、getTimestamp 或 getObject(int, Class)，否则生成阶段看到的类型
     * 与运行阶段的实际值可能不一致。
     *
     * 特别注意：getObject(int, Class) 表示请求驱动转换为指定类型，
     * 它不是 getColumnClassName(int) 所对应的默认取值方式。
     * TypeMapping 对时区类型也只按 getColumnClassName(int) 报告的 Offset 类名精确映射，
     * 不能仅根据 TIMESTAMP_WITH_TIMEZONE/TIME_WITH_TIMEZONE 就在此强制转换。
     *
     * 默认 TypeMapping 会将 Blob 映射成 byte[]、将 Clob/NClob 映射成 String，
     * 所以只在 getObject 实际返回 LOB 对象时进行物化；如果驱动已经返回
     * byte[] 或 String，则必须保留原值。
     * </pre>
     *
     * @see ResultSetMetaData#getColumnClassName(int)
     * @see ResultSet#getObject(int)
     */
    protected Object readValue(ResultSet rs, int column, int jdbcType) throws SQLException {
        // String、Integer、Long、byte[] 等高频类型走 getObject 快速路径。
        // JDBC Types 常量的数值不是类型分类；这个判断只是性能优化，其余类型仍以 getObject 兜底。
        if (jdbcType < Types.DATE) {
            return rs.getObject(column);
        }

        Object value = rs.getObject(column);
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
            byte[] data = new byte[(int) length];     // byte[] data = new byte[is.available()];
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

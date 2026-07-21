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
import cn.aifei.db.dialect.Dialect;
import cn.aifei.enjoy.util.InstanceUtil;
import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 从 ResultSet 在获取数据并放入 Row 或者 Row 的子类
 *
 * <pre>
 * 字段读取及 LOB 物化规则由 Dialect.readColumnValue(...) 提供，
 * RowFactory 负责构造 Row。
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
        Dialect dialect = dao.config().getDialect();

        while (rs.next()) {
            Map<String, Object> data = dataMapFactory.get();
            for (int i = 1; i <= columnCount; i++) {
                data.put(labelNames[i], dialect.readColumnValue(rs, i, types[i]));
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
}

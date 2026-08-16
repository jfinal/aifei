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

import cn.aifei.db.core.AifeiRow;
import cn.aifei.db.core.Dao;
import cn.aifei.db.core.DbConfig;
import cn.aifei.db.core.Row;
import cn.aifei.db.dialect.H2Dialect;
import cn.aifei.db.ext.NullDataSource;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RowFactoryTest {

    @Test
    public void rowsUseColumnLabelsCachedJdbcTypesAndConfiguredDialect() throws Exception {
        Object[][] values = {{"alice", 18}, {"bob", 20}};
        TrackingDialect dialect = new TrackingDialect(values);
        RecordingDataMapFactory dataMapFactory = new RecordingDataMapFactory();
        Dao dao = new Dao(new DbConfig("row-factory", NullDataSource.instance, dialect));
        dao.dataMapFactory(dataMapFactory);
        ResultSetFixture resultSet = new ResultSetFixture(
                new String[]{"user_name", "user_age"},
                new int[]{Types.VARCHAR, Types.INTEGER},
                values.length);

        List<Row> rows = new RowFactory().get(dao, resultSet.proxy, null);

        assertEquals(2, rows.size());
        assertEquals("alice", rows.get(0).get("user_name"));
        assertEquals(Integer.valueOf(18), rows.get(0).get("user_age"));
        assertEquals("bob", rows.get(1).get("user_name"));
        assertEquals(Integer.valueOf(20), rows.get(1).get("user_age"));
        assertSame(dataMapFactory.maps.get(0), rows.get(0).data());
        assertSame(dataMapFactory.maps.get(1), rows.get(1).data());

        assertEquals(Arrays.asList(Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.INTEGER),
                dialect.jdbcTypes);
        assertEquals(Arrays.asList(1, 2, 1, 2), dialect.columnIndexes);
        assertEquals(1, resultSet.metadata.columnCountCalls);
        assertEquals(Arrays.asList(1, 2), resultSet.metadata.labelCalls);
        assertEquals(Arrays.asList(1, 2), resultSet.metadata.typeCalls);
    }

    @Test
    public void forEachCallbackConsumesRowsWithoutAccumulatingAndMayStopIteration() throws Exception {
        Object[][] values = {{"first"}, {"second"}, {"third"}};
        TrackingDialect dialect = new TrackingDialect(values);
        Dao dao = new Dao(new DbConfig("row-callback", NullDataSource.instance, dialect));
        ResultSetFixture resultSet = new ResultSetFixture(
                new String[]{"name"}, new int[]{Types.VARCHAR}, values.length);
        List<String> visited = new ArrayList<>();

        List<Row> returned = new RowFactory().get(dao, resultSet.proxy, row -> {
            visited.add(row.get("name"));
            return visited.size() < 2;
        });

        assertTrue(returned.isEmpty());
        assertEquals(Arrays.asList("first", "second"), visited);
        assertEquals(2, resultSet.nextCalls);
        assertEquals(2, dialect.jdbcTypes.size());
    }

    @Test
    public void newRowUsesFastPathForRowAndInstantiatesCustomRowTypes() {
        TestRowFactory factory = new TestRowFactory();

        assertEquals(Row.class, factory.create(Row.class).getClass());
        assertEquals(CustomRow.class, factory.create(CustomRow.class).getClass());
    }

    public static class CustomRow extends AifeiRow<CustomRow> {
    }

    private static final class TestRowFactory extends RowFactory {

        AifeiRow<?> create(Class<? extends AifeiRow<?>> rowType) {
            return newRow(rowType);
        }
    }

    private static final class RecordingDataMapFactory extends DataMapFactory {

        final List<Map<String, Object>> maps = new ArrayList<>();

        @Override
        public Map<String, Object> get() {
            Map<String, Object> result = new LinkedHashMap<>();
            maps.add(result);
            return result;
        }
    }

    private static final class TrackingDialect extends H2Dialect {

        final Object[][] values;
        final List<Integer> columnIndexes = new ArrayList<>();
        final List<Integer> jdbcTypes = new ArrayList<>();

        TrackingDialect(Object[][] values) {
            this.values = values;
        }

        @Override
        public Object readColumnValue(ResultSet resultSet, int columnIndex, int jdbcType) {
            columnIndexes.add(columnIndex);
            jdbcTypes.add(jdbcType);
            int callIndex = jdbcTypes.size() - 1;
            int columnCount = values[0].length;
            return values[callIndex / columnCount][columnIndex - 1];
        }
    }

    private static final class ResultSetFixture implements InvocationHandler {

        final MetadataFixture metadata;
        final ResultSet proxy;
        final int rowCount;
        int cursor;
        int nextCalls;

        ResultSetFixture(String[] labels, int[] jdbcTypes, int rowCount) {
            this.metadata = new MetadataFixture(labels, jdbcTypes);
            this.rowCount = rowCount;
            this.proxy = (ResultSet) Proxy.newProxyInstance(
                    RowFactoryTest.class.getClassLoader(), new Class<?>[]{ResultSet.class}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("getMetaData".equals(method.getName())) {
                return metadata.proxy;
            }
            if ("next".equals(method.getName())) {
                nextCalls++;
                return cursor++ < rowCount;
            }
            throw new UnsupportedOperationException(method.getName());
        }
    }

    private static final class MetadataFixture implements InvocationHandler {

        final String[] labels;
        final int[] jdbcTypes;
        final ResultSetMetaData proxy;
        final List<Integer> labelCalls = new ArrayList<>();
        final List<Integer> typeCalls = new ArrayList<>();
        int columnCountCalls;

        MetadataFixture(String[] labels, int[] jdbcTypes) {
            this.labels = labels;
            this.jdbcTypes = jdbcTypes;
            this.proxy = (ResultSetMetaData) Proxy.newProxyInstance(
                    RowFactoryTest.class.getClassLoader(), new Class<?>[]{ResultSetMetaData.class}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws SQLException {
            if ("getColumnCount".equals(method.getName())) {
                columnCountCalls++;
                return labels.length;
            }
            int column = ((Number) args[0]).intValue();
            if ("getColumnLabel".equals(method.getName())) {
                labelCalls.add(column);
                return labels[column - 1];
            }
            if ("getColumnType".equals(method.getName())) {
                typeCalls.add(column);
                return jdbcTypes[column - 1];
            }
            throw new UnsupportedOperationException(method.getName());
        }
    }
}

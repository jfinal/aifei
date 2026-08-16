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

package cn.aifei.db.generator;

import cn.aifei.db.dialect.Dialect;
import cn.aifei.db.dialect.H2Dialect;
import cn.aifei.db.dialect.OracleDialect;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MetaReaderTest {

    @Test
    public void generatedTemporalTypesMatchDialectRuntimeReaders() throws Exception {
        TestMetaReader reader = new TestMetaReader();
        List<FieldInfo> fields = reader.readFields(new H2Dialect(),
                column("sql_date", String.class.getName(), Types.DATE),
                column("sql_timestamp", LocalDateTime.class.getName(), Types.TIMESTAMP),
                column("sql_time", java.sql.Time.class.getName(), Types.TIME),
                column("local_date", LocalDate.class.getName(), Types.OTHER),
                column("local_time", LocalTime.class.getName(), Types.OTHER),
                column("local_datetime", LocalDateTime.class.getName(), Types.OTHER));

        assertEquals(java.sql.Date.class.getName(), fields.get(0).javaType);
        assertEquals(java.util.Date.class.getName(), fields.get(1).javaType);
        assertEquals(java.sql.Time.class.getName(), fields.get(2).javaType);
        assertEquals(LocalDate.class.getName(), fields.get(3).javaType);
        assertEquals(LocalTime.class.getName(), fields.get(4).javaType);
        assertEquals(java.util.Date.class.getName(), fields.get(5).javaType);
    }

    @Test
    public void exactOffsetClassesAreSafeButVendorTimezoneClassesFallBackToObject() throws Exception {
        TestMetaReader reader = new TestMetaReader();
        List<FieldInfo> fields = reader.readFields(new H2Dialect(),
                column("offset_datetime", OffsetDateTime.class.getName(), Types.TIMESTAMP_WITH_TIMEZONE),
                column("offset_time", OffsetTime.class.getName(), Types.TIME_WITH_TIMEZONE),
                column("vendor_time", "org.h2.api.TimestampWithTimeZone", Types.TIMESTAMP_WITH_TIMEZONE));

        assertEquals(OffsetDateTime.class.getName(), fields.get(0).javaType);
        assertEquals(OffsetTime.class.getName(), fields.get(1).javaType);
        assertEquals("Object", fields.get(2).javaType);
    }

    @Test
    public void classNameMappingWinsThenJdbcTypeFallbackThenObjectFallback() throws Exception {
        TestMetaReader reader = new TestMetaReader();
        List<FieldInfo> fields = reader.readFields(new H2Dialect(),
                column("class_wins", String.class.getName(), Types.INTEGER),
                column("jdbc_fallback", "vendor.SmallInt", Types.SMALLINT),
                column("lob_class_wins", java.sql.Blob.class.getName(), Types.OTHER),
                column("unknown", "vendor.Unknown", Integer.MAX_VALUE));

        assertEquals("String", fields.get(0).javaType);
        assertEquals("Integer", fields.get(1).javaType);
        assertEquals("byte[]", fields.get(2).javaType);
        assertEquals("Object", fields.get(3).javaType);
    }

    @Test
    public void jdbcTypeIsReadOnceAndReusedByDialectAndFallback() throws Exception {
        TestMetaReader reader = new TestMetaReader();
        reader.getTypeMapping().addMapping("runtime.CustomValue", "example.CustomValue");
        RecordingDialect dialect = new RecordingDialect();
        Column column = column("custom_value", "metadata.IgnoredValue", Types.JAVA_OBJECT);

        List<FieldInfo> fields = reader.readFields(dialect, column);

        assertEquals("example.CustomValue", fields.get(0).javaType);
        assertEquals(Types.JAVA_OBJECT, dialect.jdbcType);
        assertEquals(1, reader.metadata.typeCalls[0]);
        assertEquals(0, reader.metadata.classNameCalls[0]);
    }

    @Test
    public void fieldMetadataUsesConfiguredAttributeConverterAndAutoIncrementFlag() throws Exception {
        TestMetaReader reader = new TestMetaReader();
        reader.setFieldToAttr((dialect, fieldName) -> "attr_" + fieldName.toLowerCase());
        reader.setReadFieldAutoIncrement(true);
        Column column = column("  USER_ID  ", Integer.class.getName(), Types.INTEGER);
        column.autoIncrement = true;

        FieldInfo field = reader.readFields(new H2Dialect(), column).get(0);

        assertEquals("USER_ID", field.name);
        assertEquals("Integer", field.javaType);
        assertEquals("attr_user_id", field.attrName);
        assertEquals(Boolean.TRUE, field.isAutoIncrement);
        assertNull(field.remarks);
    }

    @Test
    public void oracleNumberPrecisionStillRefinesBigDecimalMapping() throws Exception {
        TestMetaReader reader = new TestMetaReader();
        Column integer = column("INTEGER_VALUE", BigDecimal.class.getName(), Types.NUMERIC);
        integer.precision = 9;
        Column longValue = column("LONG_VALUE", BigDecimal.class.getName(), Types.NUMERIC);
        longValue.precision = 18;
        Column large = column("LARGE_VALUE", BigDecimal.class.getName(), Types.NUMERIC);
        large.precision = 19;
        Column decimal = column("DECIMAL_VALUE", BigDecimal.class.getName(), Types.NUMERIC);
        decimal.precision = 9;
        decimal.scale = 2;

        List<FieldInfo> fields = reader.readFields(new OracleDialect(), integer, longValue, large, decimal);

        assertEquals("Integer", fields.get(0).javaType);
        assertEquals("Long", fields.get(1).javaType);
        assertEquals(BigDecimal.class.getName(), fields.get(2).javaType);
        assertEquals(BigDecimal.class.getName(), fields.get(3).javaType);
    }

    private static Column column(String name, String className, int jdbcType) {
        return new Column(name, className, jdbcType);
    }

    private static final class RecordingDialect extends H2Dialect {

        int jdbcType;

        @Override
        public String resolveColumnValueClassName(
                ResultSetMetaData resultSetMetaData, int columnIndex, int jdbcType) {
            this.jdbcType = jdbcType;
            return "runtime.CustomValue";
        }
    }

    private static final class TestMetaReader extends MetaReader {

        MetadataHandler metadata;

        TestMetaReader() {
            setReadFieldRemarks(false);
        }

        List<FieldInfo> readFields(Dialect dialect, Column... columns) throws Exception {
            metadata = new MetadataHandler(columns);
            ResultSet resultSet = proxy(ResultSet.class, (proxy, method, args) -> {
                if ("getMetaData".equals(method.getName())) {
                    return metadata.proxy;
                }
                if ("close".equals(method.getName())) {
                    return null;
                }
                throw new UnsupportedOperationException(method.getName());
            });
            Statement statement = proxy(Statement.class, (proxy, method, args) -> {
                if ("executeQuery".equals(method.getName())) {
                    return resultSet;
                }
                if ("close".equals(method.getName())) {
                    return null;
                }
                throw new UnsupportedOperationException(method.getName());
            });
            Connection connection = proxy(Connection.class, (proxy, method, args) -> {
                if ("createStatement".equals(method.getName())) {
                    return statement;
                }
                throw new UnsupportedOperationException(method.getName());
            });
            TableInfo table = new TableInfo("metadata_test", new String[0], null, false);

            readFieldInfo(connection, null, dialect, Collections.singletonList(table));
            return table.fieldInfoList;
        }
    }

    private static final class Column {

        final String name;
        final String className;
        final int jdbcType;
        int precision;
        int scale;
        boolean autoIncrement;

        Column(String name, String className, int jdbcType) {
            this.name = name;
            this.className = className;
            this.jdbcType = jdbcType;
        }
    }

    private static final class MetadataHandler implements InvocationHandler {

        final Column[] columns;
        final int[] typeCalls;
        final int[] classNameCalls;
        final ResultSetMetaData proxy;

        MetadataHandler(Column[] columns) {
            this.columns = columns;
            this.typeCalls = new int[columns.length];
            this.classNameCalls = new int[columns.length];
            this.proxy = proxy(ResultSetMetaData.class, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("getColumnCount".equals(method.getName())) {
                return columns.length;
            }
            int index = ((Number) args[0]).intValue() - 1;
            Column column = columns[index];
            switch (method.getName()) {
                case "getColumnName":
                    return column.name;
                case "getColumnClassName":
                    classNameCalls[index]++;
                    return column.className;
                case "getColumnType":
                    typeCalls[index]++;
                    return column.jdbcType;
                case "getPrecision":
                    return column.precision;
                case "getScale":
                    return column.scale;
                case "isAutoIncrement":
                    return column.autoIncrement;
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                MetaReaderTest.class.getClassLoader(), new Class<?>[]{type}, handler);
    }
}

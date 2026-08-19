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

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DialectColumnValueTest {

    @Test
    public void defaultParameterBindingPreservesJdbcAndJavaTimeDateTypes() throws Exception {
        java.util.Date utilDate = new java.util.Date(1000L);
        Timestamp timestamp = new Timestamp(2000L);
        java.sql.Date sqlDate = new java.sql.Date(3000L);
        java.sql.Time sqlTime = new java.sql.Time(4000L);
        CustomDate customDate = new CustomDate(5000L);
        LocalDate localDate = LocalDate.of(2026, 8, 19);
        PreparedStatementRecorder recorder = new PreparedStatementRecorder();

        new OracleDialect().fillStatement(recorder.proxy,
                Arrays.asList(utilDate, timestamp, sqlDate, sqlTime, customDate, localDate, "text", null));

        assertEquals(Arrays.asList(
                "setTimestamp:1", "setTimestamp:2", "setDate:3", "setTime:4",
                "setTimestamp:5", "setObject:6", "setObject:7", "setObject:8"), recorder.methodAndIndexes());
        assertEquals(utilDate.getTime(), ((Timestamp) recorder.calls.get(0).value).getTime());
        assertSame(timestamp, recorder.calls.get(1).value);
        assertSame(sqlDate, recorder.calls.get(2).value);
        assertSame(sqlTime, recorder.calls.get(3).value);
        assertEquals(customDate.getTime(), ((Timestamp) recorder.calls.get(4).value).getTime());
        assertSame(localDate, recorder.calls.get(5).value);
        assertSame("text", recorder.calls.get(6).value);
        assertNull(recorder.calls.get(7).value);
    }

    @Test
    public void driverAwareDialectsDelegateSupportedValuesToSetObject() throws Exception {
        assertAllSetObject(new MysqlDialect());
        assertAllSetObject(new SqlServerDialect());
        assertAllSetObject(new SqliteDialect());
    }

    @Test
    public void h2UsesSetBytesOnlyForBinaryArrays() throws Exception {
        byte[] bytes = {1, 2, 3};
        java.util.Date date = new java.util.Date(1234L);
        LocalDate localDate = LocalDate.of(2026, 8, 19);
        PreparedStatementRecorder recorder = new PreparedStatementRecorder();

        new H2Dialect().fillStatement(recorder.proxy, Arrays.asList(bytes, date, localDate));

        assertEquals(Arrays.asList("setBytes:1", "setObject:2", "setObject:3"), recorder.methodAndIndexes());
        assertSame(bytes, recorder.calls.get(0).value);
        assertSame(date, recorder.calls.get(1).value);
        assertSame(localDate, recorder.calls.get(2).value);
    }

    @Test
    public void defaultReaderUsesTypedGettersOnlyForDateAndTimestamp() throws Exception {
        Object objectValue = new Object();
        java.sql.Date date = java.sql.Date.valueOf("2026-07-13");
        Timestamp timestamp = Timestamp.valueOf("2026-07-13 12:34:56.123456789");
        ResultSetRecorder resultSet = new ResultSetRecorder(objectValue, date, timestamp, false, false);
        Dialect dialect = new H2Dialect();

        assertSame(objectValue, dialect.readColumnValue(resultSet.proxy, 1, Types.VARCHAR));
        assertSame(date, dialect.readColumnValue(resultSet.proxy, 1, Types.DATE));
        assertSame(timestamp, dialect.readColumnValue(resultSet.proxy, 1, Types.TIMESTAMP));
        assertSame(objectValue, dialect.readColumnValue(resultSet.proxy, 1, Types.TIME));
        assertSame(objectValue, dialect.readColumnValue(resultSet.proxy, 1, Types.TIMESTAMP_WITH_TIMEZONE));

        assertEquals(Arrays.asList("getObject", "getDate", "getTimestamp", "getObject", "getObject"),
                resultSet.calls);
    }

    @Test
    public void lobReaderMaterializesDriverLobsAndPreservesAlreadyMaterializedValues() throws Exception {
        Dialect dialect = new H2Dialect();
        byte[] expected = {1, 2, 3, 4};
        Clob clob = clob("clob value");

        assertArrayEquals(expected, (byte[]) dialect.readColumnValue(
                new ResultSetRecorder(blob(expected, expected.length), null, null, false, false).proxy,
                1, Types.BLOB));
        assertEquals("clob value", dialect.readColumnValue(
                new ResultSetRecorder(clob, null, null, false, false).proxy, 1, Types.CLOB));
        assertEquals("clob value", dialect.readColumnValue(
                new ResultSetRecorder(clob, null, null, false, false).proxy, 1, Types.NCLOB));

        byte[] materializedBytes = {5, 6};
        String materializedString = "already materialized";
        assertSame(materializedBytes, dialect.readColumnValue(
                new ResultSetRecorder(materializedBytes, null, null, false, false).proxy, 1, Types.BLOB));
        assertSame(materializedString, dialect.readColumnValue(
                new ResultSetRecorder(materializedString, null, null, false, false).proxy, 1, Types.CLOB));
    }

    @Test
    public void blobMaterializationHandlesPartialReadsEmptyValuesAndSizeLimits() throws Exception {
        Dialect dialect = new H2Dialect();
        byte[] actual = {1, 2, 3};

        assertArrayEquals(actual, dialect.handleBlob(blob(actual, 5)));
        assertArrayEquals(new byte[0], dialect.handleBlob(blob(new byte[0], 0)));
        assertNull(dialect.handleBlob(null));
        assertNull(dialect.handleBlob(blobWithNullStream(3)));

        assertSqlException("too large", () -> dialect.handleBlob(blob(new byte[0], (long) Integer.MAX_VALUE + 1)));
        assertSqlException("Failed to read Blob data", () -> dialect.handleBlob(blobWithBrokenStream()));
    }

    @Test
    public void clobMaterializationHandlesNullAndRejectsOversizedValues() throws Exception {
        Dialect dialect = new H2Dialect();

        assertEquals("", dialect.handleClob(clob("")));
        assertNull(dialect.handleClob(null));
        assertSqlException("too large", () -> dialect.handleClob(clobWithLength((long) Integer.MAX_VALUE + 1)));
    }

    @Test
    public void sqliteBooleanReaderDistinguishesFalseFromSqlNullAndDelegatesOtherTypes() throws Exception {
        SqliteDialect dialect = new SqliteDialect();
        ResultSetRecorder trueValue = new ResultSetRecorder(null, null, null, true, false);
        ResultSetRecorder falseValue = new ResultSetRecorder(null, null, null, false, false);
        ResultSetRecorder nullValue = new ResultSetRecorder(null, null, null, false, true);
        Timestamp timestamp = Timestamp.valueOf("2026-07-13 12:34:56");
        ResultSetRecorder timestampValue = new ResultSetRecorder(null, null, timestamp, false, false);

        assertEquals(Boolean.TRUE, dialect.readColumnValue(trueValue.proxy, 1, Types.BOOLEAN));
        assertEquals(Boolean.FALSE, dialect.readColumnValue(falseValue.proxy, 1, Types.BOOLEAN));
        assertNull(dialect.readColumnValue(nullValue.proxy, 1, Types.BOOLEAN));
        assertSame(timestamp, dialect.readColumnValue(timestampValue.proxy, 1, Types.TIMESTAMP));
    }

    private static void assertAllSetObject(Dialect dialect) throws Exception {
        byte[] bytes = {1, 2};
        java.util.Date date = new java.util.Date(1234L);
        LocalDate localDate = LocalDate.of(2026, 8, 19);
        PreparedStatementRecorder recorder = new PreparedStatementRecorder();

        dialect.fillStatement(recorder.proxy, Arrays.asList(bytes, date, localDate, null));

        assertEquals(Arrays.asList("setObject:1", "setObject:2", "setObject:3", "setObject:4"),
                recorder.methodAndIndexes());
        assertSame(bytes, recorder.calls.get(0).value);
        assertSame(date, recorder.calls.get(1).value);
        assertSame(localDate, recorder.calls.get(2).value);
        assertNull(recorder.calls.get(3).value);
    }

    private static Blob blob(byte[] data, long reportedLength) {
        return proxy(Blob.class, (proxy, method, args) -> {
            if ("length".equals(method.getName())) {
                return reportedLength;
            }
            if ("getBinaryStream".equals(method.getName())) {
                return oneByteAtATime(data);
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private static Blob blobWithNullStream(long length) {
        return proxy(Blob.class, (proxy, method, args) -> {
            if ("length".equals(method.getName())) {
                return length;
            }
            if ("getBinaryStream".equals(method.getName())) {
                return null;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private static Blob blobWithBrokenStream() {
        return proxy(Blob.class, (proxy, method, args) -> {
            if ("length".equals(method.getName())) {
                return 1L;
            }
            if ("getBinaryStream".equals(method.getName())) {
                return new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("broken");
                    }

                    @Override
                    public int read(byte[] target, int offset, int length) throws IOException {
                        throw new IOException("broken");
                    }
                };
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private static Clob clob(String data) {
        return proxy(Clob.class, (proxy, method, args) -> {
            if ("length".equals(method.getName())) {
                return (long) data.length();
            }
            if ("getSubString".equals(method.getName())) {
                int start = ((Number) args[0]).intValue() - 1;
                int length = ((Number) args[1]).intValue();
                return data.substring(start, start + length);
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private static Clob clobWithLength(long length) {
        return proxy(Clob.class, (proxy, method, args) -> {
            if ("length".equals(method.getName())) {
                return length;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private static InputStream oneByteAtATime(byte[] data) {
        return new InputStream() {
            private int index;

            @Override
            public int read() {
                return index < data.length ? data[index++] & 0xff : -1;
            }

            @Override
            public int read(byte[] target, int offset, int length) {
                if (index >= data.length) {
                    return -1;
                }
                target[offset] = data[index++];
                return 1;
            }
        };
    }

    private static void assertSqlException(String messagePart, SqlAction action) {
        try {
            action.run();
            fail("SQLException expected");
        } catch (SQLException actual) {
            assertTrue("Unexpected message: " + actual.getMessage(), actual.getMessage().contains(messagePart));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                DialectColumnValueTest.class.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private interface SqlAction {
        void run() throws SQLException;
    }

    private static final class CustomDate extends java.util.Date {
        CustomDate(long time) {
            super(time);
        }
    }

    private static final class Call {
        final String method;
        final int index;
        final Object value;

        Call(String method, int index, Object value) {
            this.method = method;
            this.index = index;
            this.value = value;
        }
    }

    private static final class PreparedStatementRecorder implements InvocationHandler {

        final List<Call> calls = new ArrayList<>();
        final PreparedStatement proxy = proxy(PreparedStatement.class, this);

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getName().startsWith("set")) {
                calls.add(new Call(method.getName(), (Integer) args[0], args[1]));
                return null;
            }
            throw new UnsupportedOperationException(method.getName());
        }

        List<String> methodAndIndexes() {
            List<String> result = new ArrayList<>();
            for (Call call : calls) {
                result.add(call.method + ":" + call.index);
            }
            return result;
        }
    }

    private static final class ResultSetRecorder implements InvocationHandler {

        final Object objectValue;
        final java.sql.Date dateValue;
        final Timestamp timestampValue;
        final boolean booleanValue;
        final boolean wasNull;
        final List<String> calls = new ArrayList<>();
        final ResultSet proxy = proxy(ResultSet.class, this);

        ResultSetRecorder(Object objectValue, java.sql.Date dateValue, Timestamp timestampValue,
                          boolean booleanValue, boolean wasNull) {
            this.objectValue = objectValue;
            this.dateValue = dateValue;
            this.timestampValue = timestampValue;
            this.booleanValue = booleanValue;
            this.wasNull = wasNull;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            calls.add(method.getName());
            switch (method.getName()) {
                case "getObject":
                    return objectValue;
                case "getDate":
                    return dateValue;
                case "getTimestamp":
                    return timestampValue;
                case "getBoolean":
                    return booleanValue;
                case "wasNull":
                    return wasNull;
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        }
    }
}

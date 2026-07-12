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

import org.junit.Test;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.Types;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class RowFactoryTest {

    private final TestRowFactory factory = new TestRowFactory();

    @Test
    public void readValuePreservesGetObjectType() throws Exception {
        Object driverValue = new Object();
        byte[] driverBytes = new byte[]{1, 2};
        ResultSet rs = resultSet(driverValue);

        assertSame(driverValue, factory.read(rs, Types.VARCHAR));
        assertSame(driverValue, factory.read(rs, Types.INTEGER));
        assertSame(driverBytes, factory.read(resultSet(driverBytes), Types.BINARY));
        assertSame(driverBytes, factory.read(resultSet(driverBytes), Types.VARBINARY));
        assertSame(driverValue, factory.read(rs, Types.DATE));
        assertSame(driverValue, factory.read(rs, Types.TIME));
        assertSame(driverValue, factory.read(rs, Types.TIMESTAMP));
        assertSame(driverValue, factory.read(rs, Types.TIME_WITH_TIMEZONE));
        assertSame(driverValue, factory.read(rs, Types.TIMESTAMP_WITH_TIMEZONE));
        assertSame(driverValue, factory.read(rs, Types.OTHER));
    }

    @Test
    public void readValueOnlyMaterializesActualLobObjects() throws Exception {
        byte[] expected = new byte[]{1, 2, 3, 4};
        byte[] driverBytes = new byte[]{5, 6};
        String driverString = "already materialized";

        assertArrayEquals(expected, (byte[]) factory.read(resultSet(blob(expected)), Types.BLOB));
        Clob clob = clob("clob value");
        assertEquals("clob value", factory.read(resultSet(clob), Types.CLOB));
        assertEquals("clob value", factory.read(resultSet(clob), Types.NCLOB));
        assertSame(driverBytes, factory.read(resultSet(driverBytes), Types.BLOB));
        assertSame(driverString, factory.read(resultSet(driverString), Types.CLOB));
    }

    @Test
    public void handleBlobReadsUntilEndOfStream() throws Exception {
        byte[] expected = new byte[]{1, 2, 3, 4};
        Blob blob = blob(expected);

        assertArrayEquals(expected, factory.readBlob(blob));
        assertEquals(0, factory.readBlob(blob(new byte[0])).length);
    }

    private Blob blob(byte[] data) {
        return (Blob) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Blob.class},
                (proxy, method, args) -> {
                    if ("length".equals(method.getName())) {
                        return (long) data.length;
                    }
                    if ("getBinaryStream".equals(method.getName())) {
                        return oneByteAtATime(data);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private Clob clob(String data) {
        return (Clob) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Clob.class},
                (proxy, method, args) -> {
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

    private ResultSet resultSet(Object value) {
        return (ResultSet) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> {
                    if ("getObject".equals(method.getName()) && args.length == 1) {
                        return value;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private InputStream oneByteAtATime(byte[] data) {
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

    private static class TestRowFactory extends RowFactory {
        Object read(ResultSet rs, int jdbcType) throws Exception {
            return readValue(rs, 1, jdbcType);
        }

        byte[] readBlob(Blob blob) throws Exception {
            return handleBlob(blob);
        }
    }
}

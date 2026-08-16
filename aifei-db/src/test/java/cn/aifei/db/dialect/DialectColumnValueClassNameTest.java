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
import java.lang.reflect.Proxy;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import static org.junit.Assert.assertEquals;

public class DialectColumnValueClassNameTest {

    private final Dialect dialect = new H2Dialect();

    @Test
    public void dateAndTimestampFollowTypedRuntimeReaders() throws Exception {
        assertEquals(java.sql.Date.class.getName(),
                dialect.resolveColumnValueClassName(metadata(String.class.getName()), 1, Types.DATE));
        assertEquals(java.sql.Timestamp.class.getName(),
                dialect.resolveColumnValueClassName(metadata(String.class.getName()), 1, Types.TIMESTAMP));
    }

    @Test
    public void otherTypesFollowGetObjectMetadata() throws Exception {
        assertEquals(java.time.LocalTime.class.getName(),
                dialect.resolveColumnValueClassName(metadata(java.time.LocalTime.class.getName()), 1, Types.TIME));
        assertEquals("vendor.TimestampWithTimeZone",
                dialect.resolveColumnValueClassName(metadata("vendor.TimestampWithTimeZone"), 1, Types.TIMESTAMP_WITH_TIMEZONE));
    }

    @Test
    public void sqliteBooleanFollowsItsTypedRuntimeReader() throws Exception {
        Dialect sqliteDialect = new SqliteDialect();
        assertEquals(Boolean.class.getName(),
                sqliteDialect.resolveColumnValueClassName(metadata(Integer.class.getName()), 1, Types.BOOLEAN));
    }

    private ResultSetMetaData metadata(String className) {
        return (ResultSetMetaData) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ResultSetMetaData.class},
                (proxy, method, args) -> {
                    if ("getColumnClassName".equals(method.getName())) {
                        return className;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}

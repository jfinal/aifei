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
                dialect.getColumnValueClassName(metadata(String.class.getName(), Types.DATE), 1));
        assertEquals(java.sql.Timestamp.class.getName(),
                dialect.getColumnValueClassName(metadata(String.class.getName(), Types.TIMESTAMP), 1));
    }

    @Test
    public void otherTypesFollowGetObjectMetadata() throws Exception {
        assertEquals(java.time.LocalTime.class.getName(),
                dialect.getColumnValueClassName(metadata(java.time.LocalTime.class.getName(), Types.TIME), 1));
        assertEquals("vendor.TimestampWithTimeZone",
                dialect.getColumnValueClassName(metadata("vendor.TimestampWithTimeZone", Types.TIMESTAMP_WITH_TIMEZONE), 1));
    }

    @Test
    public void sqliteBooleanFollowsItsTypedRuntimeReader() throws Exception {
        Dialect sqliteDialect = new SqliteDialect();
        assertEquals(Boolean.class.getName(),
                sqliteDialect.getColumnValueClassName(metadata(Integer.class.getName(), Types.BOOLEAN), 1));
    }

    private ResultSetMetaData metadata(String className, int jdbcType) {
        return (ResultSetMetaData) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ResultSetMetaData.class},
                (proxy, method, args) -> {
                    if ("getColumnType".equals(method.getName())) {
                        return jdbcType;
                    }
                    if ("getColumnClassName".equals(method.getName())) {
                        return className;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}

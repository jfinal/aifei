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
import java.math.BigDecimal;
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

    @Test
    public void sqliteFloatingPointTypesFollowDoubleRuntimeValues() throws Exception {
        Dialect sqliteDialect = new SqliteDialect();

        assertEquals(Double.class.getName(),
                sqliteDialect.resolveColumnValueClassName(metadata(Object.class.getName()), 1, Types.REAL));
        assertEquals(Double.class.getName(),
                sqliteDialect.resolveColumnValueClassName(metadata(Object.class.getName()), 1, Types.FLOAT));
        assertEquals(Double.class.getName(),
                sqliteDialect.resolveColumnValueClassName(metadata(Object.class.getName()), 1, Types.DOUBLE));
    }

    @Test
    public void oracleNumberUsesPrecisionAndScale() throws Exception {
        Dialect oracleDialect = new OracleDialect();

        assertEquals(Integer.class.getName(),
                oracleDialect.resolveColumnValueClassName(metadata(BigDecimal.class.getName(), 9, 0), 1, Types.NUMERIC));
        assertEquals(Long.class.getName(),
                oracleDialect.resolveColumnValueClassName(metadata(BigDecimal.class.getName(), 18, 0), 1, Types.NUMERIC));
        assertEquals(BigDecimal.class.getName(),
                oracleDialect.resolveColumnValueClassName(metadata(BigDecimal.class.getName(), 19, 0), 1, Types.NUMERIC));
        assertEquals(BigDecimal.class.getName(),
                oracleDialect.resolveColumnValueClassName(metadata(BigDecimal.class.getName(), 9, 2), 1, Types.NUMERIC));
    }

    @Test
    public void oracleNumberKeepsBigDecimalWhenMetadataCannotSafelyNarrow() throws Exception {
        Dialect oracleDialect = new OracleDialect();

        assertEquals(BigDecimal.class.getName(),
                oracleDialect.resolveColumnValueClassName(metadata(BigDecimal.class.getName(), 0, 0), 1, Types.NUMERIC));
        assertEquals(BigDecimal.class.getName(),
                oracleDialect.resolveColumnValueClassName(metadata(BigDecimal.class.getName(), 0, -127), 1, Types.NUMERIC));
        assertEquals(BigDecimal.class.getName(),
                oracleDialect.resolveColumnValueClassName(metadata(BigDecimal.class.getName(), 6, -2), 1, Types.NUMERIC));
    }

    private ResultSetMetaData metadata(String className) {
        return metadata(className, 0, 0);
    }

    private ResultSetMetaData metadata(String className, int precision, int scale) {
        return (ResultSetMetaData) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ResultSetMetaData.class},
                (proxy, method, args) -> {
                    if ("getColumnClassName".equals(method.getName())) {
                        return className;
                    } else if ("getPrecision".equals(method.getName())) {
                        return precision;
                    } else if ("getScale".equals(method.getName())) {
                        return scale;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}

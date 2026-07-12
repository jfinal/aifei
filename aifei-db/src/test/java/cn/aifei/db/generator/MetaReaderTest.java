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

import cn.aifei.db.dialect.H2Dialect;
import org.junit.Test;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Collections;
import static org.junit.Assert.assertEquals;

public class MetaReaderTest {

    private final TestMetaReader metaReader = new TestMetaReader();

    @Test
    public void exactOffsetClassNamesGenerateOffsetTypes() throws Exception {
        assertEquals(OffsetDateTime.class.getName(),
                metaReader.readJavaType(OffsetDateTime.class.getName(), Types.TIMESTAMP_WITH_TIMEZONE));
        assertEquals(OffsetTime.class.getName(),
                metaReader.readJavaType(OffsetTime.class.getName(), Types.TIME_WITH_TIMEZONE));
    }

    @Test
    public void vendorTimezoneClassDoesNotGenerateUnsafeOffsetGetter() throws Exception {
        // H2 1.4 默认行为：className 是厂商类，JDBC type 是 TIMESTAMP_WITH_TIMEZONE。
        assertEquals("Object", metaReader.readJavaType(
                "org.h2.api.TimestampWithTimeZone", Types.TIMESTAMP_WITH_TIMEZONE));
    }

    private static class TestMetaReader extends MetaReader {

        String readJavaType(String className, int jdbcType) throws Exception {
            setReadFieldRemarks(false);
            ResultSetMetaData metadata = metadata(className, jdbcType);
            ResultSet resultSet = resultSet(metadata);
            Statement statement = statement(resultSet);
            Connection connection = connection(statement);
            TableInfo table = new TableInfo("timezone_test", new String[0], null, false);

            readFieldInfo(connection, null, new H2Dialect(), Collections.singletonList(table));
            return table.fieldInfoList.get(0).javaType;
        }

        private ResultSetMetaData metadata(String className, int jdbcType) {
            return (ResultSetMetaData) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{ResultSetMetaData.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getColumnCount":
                                return 1;
                            case "getColumnName":
                                return "event_time";
                            case "getColumnClassName":
                                return className;
                            case "getColumnType":
                                return jdbcType;
                            default:
                                throw new UnsupportedOperationException(method.getName());
                        }
                    });
        }

        private ResultSet resultSet(ResultSetMetaData metadata) {
            return (ResultSet) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> {
                        if ("getMetaData".equals(method.getName())) {
                            return metadata;
                        }
                        if ("close".equals(method.getName())) {
                            return null;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
        }

        private Statement statement(ResultSet resultSet) {
            return (Statement) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Statement.class},
                    (proxy, method, args) -> {
                        if ("executeQuery".equals(method.getName())) {
                            return resultSet;
                        }
                        if ("close".equals(method.getName())) {
                            return null;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
        }

        private Connection connection(Statement statement) {
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("createStatement".equals(method.getName())) {
                            return statement;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}

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

package cn.aifei.db.executor;

import cn.aifei.db.core.AifeiDao;
import cn.aifei.db.core.AifeiRow;
import cn.aifei.db.core.Dao;
import cn.aifei.db.core.DbConfig;
import cn.aifei.db.core.Page;
import cn.aifei.db.core.Row;
import cn.aifei.db.ext.NullDataSource;
import cn.aifei.db.factory.RowFactory;
import cn.aifei.log.Log;
import cn.aifei.log.LogFactory;
import cn.aifei.log.LogKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PaginateExecutorTest {

    private static LogFactory originalLogFactory;

    @BeforeClass
    public static void installNoOpLogFactory() {
        originalLogFactory = LogKit.get().getLogFactory();
        Log log = proxy(Log.class, (instance, method, args) ->
                method.getReturnType() == boolean.class ? false : null);
        LogKit.get().setLogFactory(proxy(LogFactory.class, (instance, method, args) -> log));
    }

    @AfterClass
    public static void restoreLogFactory() {
        LogKit.get().setLogFactory(originalLogFactory);
    }

    @Test
    public void firstPageIsQueriedWhenTotalPagesExceedsIntRange() {
        assertFirstPage(3_000_000_000L, 1, 3_000_000_000L);
        assertFirstPage(4_294_967_295L, 2, 2_147_483_648L);
    }

    @Test
    public void firstPageIsQueriedWithLongMaxValueTotalRows() {
        assertFirstPage(Long.MAX_VALUE, 1, Long.MAX_VALUE);
        assertFirstPage(Long.MAX_VALUE, 2, 4_611_686_018_427_387_904L);
    }

    @Test
    public void emptyAndOutOfRangePagesSkipTheDataQuery() {
        Fixture emptyFixture = new Fixture();
        Page<Row> empty = emptyFixture.paginate(1, 10, 0);
        assertEquals(0L, empty.getTotalPages());
        assertTrue(empty.getRows().isEmpty());
        assertEquals(0, emptyFixture.queryCount);

        Fixture outOfRangeFixture = new Fixture();
        Page<Row> outOfRange = outOfRangeFixture.paginate(3, 10, 20);
        assertEquals(3, outOfRange.getPageNum());
        assertEquals(20L, outOfRange.getTotalRows());
        assertEquals(2L, outOfRange.getTotalPages());
        assertTrue(outOfRange.getRows().isEmpty());
        assertEquals(0, outOfRangeFixture.queryCount);
    }

    private void assertFirstPage(long totalRows, int pageSize, long totalPages) {
        Fixture fixture = new Fixture();
        Page<Row> page = fixture.paginate(1, pageSize, totalRows);

        assertEquals(1, page.getPageNum());
        assertEquals(pageSize, page.getPageSize());
        assertEquals(totalRows, page.getTotalRows());
        assertEquals(totalPages, page.getTotalPages());
        assertEquals(1, fixture.queryCount);
        assertEquals(1, page.getRows().size());
        assertSame(fixture.row, page.getRows().get(0));
        assertTrue(page.firstPage());
        assertFalse(page.lastPage());
    }

    private static final class Fixture {

        final Row row = new Row();
        final Dao dao;
        int queryCount;

        Fixture() {
            PreparedStatement statement = proxy(PreparedStatement.class, (instance, method, args) -> {
                if ("executeQuery".equals(method.getName())) {
                    queryCount++;
                    return null;
                }
                if ("close".equals(method.getName())) {
                    return null;
                }
                throw new UnsupportedOperationException(method.getName());
            });
            Connection connection = proxy(Connection.class, (instance, method, args) -> {
                if ("prepareStatement".equals(method.getName())) {
                    return statement;
                }
                if ("close".equals(method.getName())) {
                    return null;
                }
                throw new UnsupportedOperationException(method.getName());
            });
            DbConfig config = new DbConfig("paginate-long-total-pages", new NullDataSource() {
                @Override
                public Connection getConnection() {
                    return connection;
                }
            });
            config.setRowFactory(new RowFactory() {
                @Override
                public <T extends AifeiRow<T>> List<T> get(AifeiDao<?, T> dao, ResultSet resultSet,
                                                         Function<T, Boolean> forEachFun) {
                    return Collections.singletonList(dao.rowType().cast(row));
                }
            });
            dao = new Dao(config).sql("select * from example");
        }

        Page<Row> paginate(int pageNum, int pageSize, long totalRows) {
            return dao.paginate(pageNum, pageSize, (sqlPara, query) -> totalRows);
        }
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(
                PaginateExecutorTest.class.getClassLoader(), new Class<?>[]{type}, handler));
    }
}

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

import cn.aifei.db.core.SqlPara;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class DialectPaginateTest {

    private static final String FIND_SQL = "select id from example where status = ? order by id";
    private static final String ROWNUM_SQL = "SELECT * FROM ( SELECT row_.*, rownum rownum_ FROM (  "
            + FIND_SQL + " ) row_ WHERE rownum <= %2$d) table_alias WHERE table_alias.rownum_ > %1$d";

    @Test
    public void mysqlPreservesLargeOffsets() {
        assertPagination(new MysqlDialect(), FIND_SQL + " LIMIT %1$d, %3$d");
    }

    @Test
    public void postgreSqlPreservesLargeOffsets() {
        assertPagination(new PostgreSqlDialect(), FIND_SQL + " LIMIT %3$d OFFSET %1$d");
    }

    @Test
    public void sqlitePreservesLargeOffsets() {
        assertPagination(new SqliteDialect(), FIND_SQL + " LIMIT %1$d, %3$d");
    }

    @Test
    public void oraclePreservesLargeRowBounds() {
        assertPagination(new OracleDialect(), ROWNUM_SQL);
    }

    @Test
    public void h2PreservesLargeRowBounds() {
        assertPagination(new H2Dialect(), ROWNUM_SQL);
    }

    @Test
    public void sqlServerPreservesLargeRowBounds() {
        assertPagination(new SqlServerDialect(),
                "SELECT * FROM ( SELECT row_number() OVER (ORDER BY tempcolumn) temprownumber, * FROM "
                + " ( SELECT TOP %2$d tempcolumn=0, id from example where status = ? order by id)vip)mvp"
                + " WHERE temprownumber>%1$d");
    }

    @Test
    public void informixPreservesLargeOffsets() {
        assertPagination(new InformixDialect(),
                "SELECT skip %1$d first %3$d  id from example where status = ? order by id");
    }

    private void assertPagination(Dialect dialect, String expectedSqlFormat) {
        assertPage(dialect, expectedSqlFormat, 1, 10, 0L, 10L);
        assertPage(dialect, expectedSqlFormat, 3, 10, 20L, 30L);
        assertPage(dialect, expectedSqlFormat, 2, 1_073_741_824, 1_073_741_824L, 2_147_483_648L);
        assertPage(dialect, expectedSqlFormat, 3, 1_073_741_824, 2_147_483_648L, 3_221_225_472L);
        assertPage(dialect, expectedSqlFormat, Integer.MAX_VALUE, Integer.MAX_VALUE,
                4_611_686_011_984_936_962L, 4_611_686_014_132_420_609L);
    }

    private void assertPage(Dialect dialect, String expectedSqlFormat, int pageNum, int pageSize,
                            long start, long end) {
        SqlPara source = new SqlPara(FIND_SQL, "active");

        SqlPara result = dialect.paginate(pageNum, pageSize, source);

        String message = dialect.getClass().getSimpleName() + ": pageNum=" + pageNum + ", pageSize=" + pageSize;
        assertEquals(message, String.format(Locale.ROOT, expectedSqlFormat, start, end, pageSize), result.getSql());
        assertSame(source.getPara(), result.getPara());
        assertEquals(FIND_SQL, source.getSql());
    }
}

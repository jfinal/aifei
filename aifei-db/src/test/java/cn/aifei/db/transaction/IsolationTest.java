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

package cn.aifei.db.transaction;

import org.junit.Test;

import java.sql.Connection;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IsolationTest {

    @Test
    public void fromResolvesAllSupportedJdbcIsolationLevels() {
        assertSame(Isolation.READ_UNCOMMITTED, Isolation.from(Connection.TRANSACTION_READ_UNCOMMITTED));
        assertSame(Isolation.READ_COMMITTED, Isolation.from(Connection.TRANSACTION_READ_COMMITTED));
        assertSame(Isolation.REPEATABLE_READ, Isolation.from(Connection.TRANSACTION_REPEATABLE_READ));
        assertSame(Isolation.SERIALIZABLE, Isolation.from(Connection.TRANSACTION_SERIALIZABLE));
    }

    @Test
    public void transactionNoneIsRejectedBecauseItCannotProvideTransactionSemantics() {
        try {
            Isolation.from(Connection.TRANSACTION_NONE);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException actual) {
            assertTrue(actual.getMessage().contains("Unknown transaction isolation level: 0"));
        }
    }
}

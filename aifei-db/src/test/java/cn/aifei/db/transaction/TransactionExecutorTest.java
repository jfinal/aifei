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

import cn.aifei.db.core.AifeiDbException;
import cn.aifei.db.core.DbConfig;
import cn.aifei.log.Log;
import cn.aifei.log.LogFactory;
import cn.aifei.log.LogKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TransactionExecutorTest {

    private static LogFactory originalLogFactory;

    @BeforeClass
    public static void setUpLogFactory() {
        originalLogFactory = LogKit.get().getLogFactory();
        Log noOpLog = (Log) Proxy.newProxyInstance(
                TransactionExecutorTest.class.getClassLoader(),
                new Class<?>[]{Log.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
        LogFactory noOpLogFactory = (LogFactory) Proxy.newProxyInstance(
                TransactionExecutorTest.class.getClassLoader(),
                new Class<?>[]{LogFactory.class},
                (proxy, method, args) -> noOpLog);
        LogKit.get().setLogFactory(noOpLogFactory);
    }

    @AfterClass
    public static void restoreLogFactory() {
        LogKit.get().setLogFactory(originalLogFactory);
    }

    @Test
    public void topLevelErrorRollsBackAndKeepsIdentity() {
        ConnectionState state = new ConnectionState();
        DbConfig config = createConfig(state);
        AssertionError expected = new AssertionError("atom");

        try {
            new TransactionExecutor().execute(config, Isolation.REPEATABLE_READ, tx -> {
                throw expected;
            });
            fail();
        } catch (AssertionError actual) {
            assertSame(expected, actual);
        }

        assertEquals(1, state.rollbackCount);
        assertEquals(0, state.commitCount);
        assertEquals(1, state.restoreAutoCommitCount);
        assertEquals(0, state.implicitCommitCount);
        assertEquals(1, state.closeCount);
        assertFalse(config.getTransactionKit().isInTransaction());
    }

    @Test
    public void caughtNestedErrorStillRollsBackOuterTransaction() {
        ConnectionState state = new ConnectionState();
        DbConfig config = createConfig(state);
        TransactionExecutor executor = new TransactionExecutor();
        AssertionError expected = new AssertionError("nested");

        String result = executor.execute(config, Isolation.REPEATABLE_READ, tx -> {
            try {
                executor.execute(config, Isolation.REPEATABLE_READ, inner -> {
                    throw expected;
                });
                fail();
            } catch (AssertionError actual) {
                assertSame(expected, actual);
            }
            return "continued";
        });

        assertEquals("continued", result);
        assertEquals(1, state.rollbackCount);
        assertEquals(0, state.commitCount);
        assertEquals(0, state.implicitCommitCount);
    }

    @Test
    public void beginFailureDoesNotCleanUpUninitializedTransactionState() {
        ConnectionState state = new ConnectionState();
        SQLException beginFailure = new SQLException("begin");
        state.getTransactionIsolationFailure = beginFailure;
        DbConfig config = createConfig(state);

        try {
            new TransactionExecutor().execute(
                    config, Isolation.REPEATABLE_READ, tx -> "unused");
            fail();
        } catch (AifeiDbException actual) {
            assertSame(beginFailure, actual.getCause());
        }

        assertEquals(0, state.rollbackCount);
        assertEquals(0, state.setAutoCommitCount);
        assertEquals(0, state.setTransactionIsolationCount);
        assertEquals(1, state.closeCount);
        assertFalse(config.getTransactionKit().isInTransaction());
    }

    @Test
    public void partiallyAppliedBeginStateIsRestored() {
        ConnectionState state = new ConnectionState();
        state.isolation = Connection.TRANSACTION_READ_COMMITTED;
        SQLException beginFailure = new SQLException("setAutoCommit");
        state.setAutoCommitFailure = beginFailure;
        DbConfig config = createConfig(state);

        try {
            new TransactionExecutor().execute(
                    config, Isolation.REPEATABLE_READ, tx -> "unused");
            fail();
        } catch (AifeiDbException actual) {
            assertSame(beginFailure, actual.getCause());
        }

        assertEquals(0, state.rollbackCount);
        assertEquals(2, state.setAutoCommitCount);
        assertEquals(2, state.setTransactionIsolationCount);
        assertTrue(state.autoCommit);
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, state.isolation);
        assertEquals(1, state.closeCount);
        assertFalse(config.getTransactionKit().isInTransaction());
    }

    @Test
    public void rollbackErrorIsNotSwallowedByExceptionCallback() {
        ConnectionState state = new ConnectionState();
        AssertionError rollbackFailure = new AssertionError("rollback");
        state.rollbackFailure = rollbackFailure;
        DbConfig config = createConfig(state);
        AtomicBoolean callbackInvoked = new AtomicBoolean();
        config.setOnTransactionException(e -> {
            callbackInvoked.set(true);
            return "fallback";
        });
        Exception atomFailure = new Exception("atom");

        try {
            new TransactionExecutor().execute(config, Isolation.REPEATABLE_READ, tx -> {
                throw atomFailure;
            });
            fail();
        } catch (AssertionError actual) {
            assertSame(rollbackFailure, actual);
        }

        assertFalse(callbackInvoked.get());
        assertEquals(1, state.rollbackCount);
        assertEquals(1, state.restoreAutoCommitCount);
        assertEquals(1, state.implicitCommitCount);
        assertTrue(state.autoCommit);
        assertEquals(1, state.closeCount);
        assertFalse(config.getTransactionKit().isInTransaction());
    }

    @Test
    public void directRollbackExceptionCanBeHandledByExceptionCallback() {
        ConnectionState state = new ConnectionState();
        SQLException rollbackFailure = new SQLException("rollback");
        state.rollbackFailure = rollbackFailure;
        DbConfig config = createConfig(state);
        AtomicBoolean callbackInvoked = new AtomicBoolean();
        config.setOnTransactionException(e -> {
            callbackInvoked.set(true);
            return "fallback";
        });

        String result = new TransactionExecutor().execute(config, Isolation.REPEATABLE_READ, tx -> {
            tx.rollback();
            return "result";
        });

        assertEquals("fallback", result);
        assertTrue(callbackInvoked.get());
        assertEquals(1, state.rollbackCount);
        assertEquals(1, state.restoreAutoCommitCount);
        assertEquals(1, state.implicitCommitCount);
        assertTrue(state.autoCommit);
        assertEquals(1, state.closeCount);
    }

    @Test
    public void successfulRollbackStillAllowsExceptionCallback() {
        ConnectionState state = new ConnectionState();
        DbConfig config = createConfig(state);
        AtomicBoolean callbackInvoked = new AtomicBoolean();
        config.setOnTransactionException(e -> {
            callbackInvoked.set(true);
            return "fallback";
        });

        String result = new TransactionExecutor().execute(
                config, Isolation.REPEATABLE_READ, tx -> {
                    throw new Exception("atom");
                });

        assertEquals("fallback", result);
        assertEquals(1, state.rollbackCount);
        assertEquals(1, state.restoreAutoCommitCount);
        assertEquals(0, state.implicitCommitCount);
        assertEquals(1, state.closeCount);
        assertTrue(callbackInvoked.get());
    }

    private static DbConfig createConfig(ConnectionState state) {
        Connection connection = (Connection) Proxy.newProxyInstance(
                TransactionExecutorTest.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                state);
        return new DbConfig("transaction-test", new SingleConnectionDataSource(connection));
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }

    private static class ConnectionState implements InvocationHandler {

        boolean autoCommit = true;
        boolean transactionResolved;
        int isolation = Connection.TRANSACTION_REPEATABLE_READ;
        int rollbackCount;
        int commitCount;
        int setAutoCommitCount;
        int setTransactionIsolationCount;
        int restoreAutoCommitCount;
        int implicitCommitCount;
        int closeCount;
        Throwable rollbackFailure;
        Throwable getTransactionIsolationFailure;
        Throwable setAutoCommitFailure;
        boolean setAutoCommitFailureThrown;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("getTransactionIsolation".equals(name)) {
                if (getTransactionIsolationFailure != null) {
                    throw getTransactionIsolationFailure;
                }
                return isolation;
            }
            if ("setTransactionIsolation".equals(name)) {
                setTransactionIsolationCount++;
                isolation = (Integer) args[0];
                return null;
            }
            if ("getAutoCommit".equals(name)) {
                return autoCommit;
            }
            if ("setAutoCommit".equals(name)) {
                setAutoCommitCount++;
                boolean newValue = (Boolean) args[0];
                if (!autoCommit && newValue) {
                    restoreAutoCommitCount++;
                    if (!transactionResolved) {
                        implicitCommitCount++;
                    }
                }
                autoCommit = newValue;
                if (setAutoCommitFailure != null && !setAutoCommitFailureThrown) {
                    setAutoCommitFailureThrown = true;
                    throw setAutoCommitFailure;
                }
                return null;
            }
            if ("rollback".equals(name)) {
                rollbackCount++;
                if (rollbackFailure != null) {
                    throw rollbackFailure;
                }
                transactionResolved = true;
                return null;
            }
            if ("commit".equals(name)) {
                commitCount++;
                transactionResolved = true;
                return null;
            }
            if ("close".equals(name)) {
                closeCount++;
                return null;
            }
            if ("isWrapperFor".equals(name)) {
                return false;
            }
            if ("unwrap".equals(name)) {
                throw new SQLException();
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static class SingleConnectionDataSource implements DataSource {

        private final Connection connection;

        SingleConnectionDataSource(Connection connection) {
            this.connection = connection;
        }

        @Override
        public Connection getConnection() {
            return connection;
        }

        @Override
        public Connection getConnection(String username, String password) {
            return connection;
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException();
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }
}

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        // 回滚失败不得恢复 autoCommit，避免 setAutoCommit(true) 隐式提交本应回滚的事务
        assertEquals(0, state.restoreAutoCommitCount);
        assertEquals(0, state.implicitCommitCount);
        assertFalse(state.autoCommit);
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
        // 回滚失败不得恢复 autoCommit，避免 setAutoCommit(true) 隐式提交本应回滚的事务
        assertEquals(0, state.restoreAutoCommitCount);
        assertEquals(0, state.implicitCommitCount);
        assertFalse(state.autoCommit);
        assertEquals(1, state.closeCount);
        assertFalse(config.getTransactionKit().isInTransaction());
    }

    @Test
    public void rollbackFailureSkipsAllConnectionStateRestoration() {
        ConnectionState state = new ConnectionState();
        state.isolation = Connection.TRANSACTION_READ_COMMITTED;
        SQLException rollbackFailure = new SQLException("rollback");
        state.rollbackFailure = rollbackFailure;
        DbConfig config = createConfig(state);

        try {
            new TransactionExecutor().execute(config, Isolation.REPEATABLE_READ, tx -> {
                tx.rollback();
                return "unused";
            });
            fail();
        } catch (AifeiDbException actual) {
            assertSame(rollbackFailure, actual.getCause());
        }

        assertEquals(1, state.rollbackCount);
        assertEquals(0, state.restoreAutoCommitCount);
        assertFalse(state.autoCommit);
        assertEquals(1, state.setTransactionIsolationCount);   // 仅 begin() 设置，end() 不得恢复
        assertEquals(Connection.TRANSACTION_REPEATABLE_READ, state.isolation);
        assertEquals(1, state.closeCount);
        assertFalse(config.getTransactionKit().isInTransaction());
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

    @Test
    public void commitSuccessCallbackRunsAfterConnectionClosedAndThreadLocalCleared() {
        ConnectionState state = new ConnectionState();
        DbConfig config = createConfig(state);
        AtomicBoolean callbackInvoked = new AtomicBoolean();

        String result = new TransactionExecutor().execute(config, Isolation.REPEATABLE_READ, tx -> {
            tx.onCommitSuccess(() -> {
                callbackInvoked.set(true);
                assertEquals(1, state.closeCount);                          // 连接已关闭
                assertTrue(state.autoCommit);                               // autoCommit 已恢复
                assertFalse(config.getTransactionKit().isInTransaction());  // ThreadLocal 已清理
            });
            return "result";
        });

        assertEquals("result", result);
        assertTrue(callbackInvoked.get());
        assertEquals(1, state.commitCount);
        assertEquals(0, state.rollbackCount);
    }

    @Test
    public void commitSuccessCallbackCanStartFreshTransaction() {
        ConnectionState state = new ConnectionState();
        DbConfig config = createConfig(state);
        TransactionExecutor executor = new TransactionExecutor();
        AtomicBoolean innerInvoked = new AtomicBoolean();

        String result = executor.execute(config, Isolation.REPEATABLE_READ, tx -> {
            tx.onCommitSuccess(() -> {
                // 回调中开启的事务是独立的顶层事务，不复用已提交事务的连接
                String inner = executor.execute(config, Isolation.REPEATABLE_READ, innerTx -> {
                    innerInvoked.set(true);
                    assertTrue(config.getTransactionKit().isInTransaction());
                    return "inner";
                });
                assertEquals("inner", inner);
            });
            return "outer";
        });

        assertEquals("outer", result);
        assertTrue(innerInvoked.get());
        assertEquals(2, state.commitCount);     // 外层事务与回调中的新事务各自提交
        assertEquals(2, state.closeCount);
        assertFalse(config.getTransactionKit().isInTransaction());
    }

    @Test
    public void commitSuccessCallbackRunsAfterConnectionCloseFailureAndThreadLocalCleared() {
        ConnectionState state = new ConnectionState();
        SQLException closeFailure = new SQLException("close");
        state.closeFailure = closeFailure;
        DbConfig config = createConfig(state);
        AtomicBoolean callbackInvoked = new AtomicBoolean();

        String result = new TransactionExecutor().execute(config, Isolation.REPEATABLE_READ, tx -> {
            tx.onCommitSuccess(() -> {
                callbackInvoked.set(true);
                assertEquals(1, state.closeCount);                          // 已尝试关闭连接
                assertFalse(config.getTransactionKit().isInTransaction());  // ThreadLocal 已清理
            });
            return "result";
        });

        assertEquals("result", result);
        assertTrue(callbackInvoked.get());
        assertEquals(1, state.commitCount);
        assertEquals(1, state.closeCount);
        assertFalse(config.getTransactionKit().isInTransaction());
    }

    @Test
    public void rollbackDoesNotTriggerCommitSuccessCallback() {
        ConnectionState state = new ConnectionState();
        DbConfig config = createConfig(state);
        AtomicBoolean callbackInvoked = new AtomicBoolean();

        String result = new TransactionExecutor().execute(config, Isolation.REPEATABLE_READ, tx -> {
            tx.onCommitSuccess(() -> callbackInvoked.set(true));
            tx.rollback();
            return "rolledBack";
        });

        assertEquals("rolledBack", result);
        assertFalse(callbackInvoked.get());
        assertEquals(0, state.commitCount);
        assertEquals(1, state.rollbackCount);
    }

    @Test
    public void commitFailureDoesNotTriggerCommitSuccessCallback() {
        ConnectionState state = new ConnectionState();
        SQLException commitFailure = new SQLException("commit");
        state.commitFailure = commitFailure;
        DbConfig config = createConfig(state);
        AtomicBoolean callbackInvoked = new AtomicBoolean();

        try {
            new TransactionExecutor().execute(config, Isolation.REPEATABLE_READ, tx -> {
                tx.onCommitSuccess(() -> callbackInvoked.set(true));
                return "unused";
            });
            fail();
        } catch (AifeiDbException actual) {
            assertSame(commitFailure, actual.getCause());
        }

        assertFalse(callbackInvoked.get());
        assertEquals(1, state.commitCount);     // commit 被调用但失败
        assertEquals(1, state.rollbackCount);   // 提交失败后回滚事务
        assertEquals(1, state.closeCount);
        assertFalse(config.getTransactionKit().isInTransaction());
    }

    @Test
    public void businessExceptionDoesNotTriggerCommitSuccessCallback() {
        ConnectionState state = new ConnectionState();
        DbConfig config = createConfig(state);
        AtomicBoolean callbackInvoked = new AtomicBoolean();
        Exception atomFailure = new Exception("atom");

        try {
            new TransactionExecutor().execute(config, Isolation.REPEATABLE_READ, tx -> {
                tx.onCommitSuccess(() -> callbackInvoked.set(true));
                throw atomFailure;
            });
            fail();
        } catch (AifeiDbException actual) {
            assertSame(atomFailure, actual.getCause());
        }

        assertFalse(callbackInvoked.get());
        assertEquals(0, state.commitCount);
        assertEquals(1, state.rollbackCount);
    }

    @Test
    public void failingCommitSuccessCallbackDoesNotAffectResultOrOtherCallbacks() {
        ConnectionState state = new ConnectionState();
        DbConfig config = createConfig(state);
        List<String> invoked = new ArrayList<>();

        String result = new TransactionExecutor().execute(config, Isolation.REPEATABLE_READ, tx -> {
            tx.onCommitSuccess(() -> invoked.add("first"));
            tx.onCommitSuccess(() -> {                          // 后注册的先执行
                invoked.add("second");
                throw new RuntimeException("callback");
            });
            return "result";
        });

        assertEquals("result", result);                         // 回调异常不影响事务返回值
        assertEquals(1, state.commitCount);
        assertEquals(Arrays.asList("second", "first"), invoked);  // 逆序执行，且其它回调仍被执行
    }

    @Test
    public void appliedThenFailedNestedIsolationUpgradeIsRestored() {
        ConnectionState state = new ConnectionState();
        // 模拟驱动先应用新的隔离级别再抛出异常
        state.applyIsolationBeforeThrow = true;
        state.setTransactionIsolationFailureValue = Connection.TRANSACTION_SERIALIZABLE;
        SQLException isolationFailure = new SQLException("setTransactionIsolation");
        state.setTransactionIsolationFailure = isolationFailure;
        DbConfig config = createConfig(state);
        TransactionExecutor executor = new TransactionExecutor();

        String result = executor.execute(config, Isolation.REPEATABLE_READ, tx -> {
            try {
                executor.execute(config, Isolation.SERIALIZABLE, inner -> "unused");
                fail();
            } catch (AifeiDbException expected) {
                assertSame(isolationFailure, expected.getCause());
            }
            return "continued";
        });

        assertEquals("continued", result);
        assertEquals(2, state.setTransactionIsolationCount);                // 嵌套尝试 + end() 恢复
        assertEquals(Connection.TRANSACTION_REPEATABLE_READ, state.isolation);  // 已恢复原始隔离级别
        assertEquals(1, state.rollbackCount);
        assertEquals(1, state.closeCount);
        assertFalse(config.getTransactionKit().isInTransaction());
    }

    @Test
    public void nestedExceptionCallbackFailureDoesNotMaskOriginalException() {
        ConnectionState state = new ConnectionState();
        DbConfig config = createConfig(state);
        TransactionExecutor executor = new TransactionExecutor();
        Exception original = new Exception("original");
        AtomicBoolean callbackInvoked = new AtomicBoolean();

        try {
            executor.execute(config, Isolation.REPEATABLE_READ, tx -> {
                executor.execute(config, Isolation.REPEATABLE_READ, inner -> {
                    inner.onException(e -> {
                        callbackInvoked.set(true);
                        throw new RuntimeException("callback");     // 回调自身抛异常
                    });
                    throw original;
                });
                fail();
                return null;
            });
            fail();
        } catch (AifeiDbException actual) {
            assertSame(original, actual.getCause());    // 原始异常未被回调异常掩盖
        }

        assertTrue(callbackInvoked.get());
        assertEquals(1, state.rollbackCount);
        assertEquals(0, state.commitCount);
        assertFalse(config.getTransactionKit().isInTransaction());
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
        Throwable commitFailure;
        Throwable closeFailure;
        Throwable getTransactionIsolationFailure;
        Throwable setAutoCommitFailure;
        boolean setAutoCommitFailureThrown;
        Throwable setTransactionIsolationFailure;
        Integer setTransactionIsolationFailureValue;    // 仅对传入该值的调用抛出异常
        boolean setTransactionIsolationFailureThrown;   // 异常只抛出一次，避免影响 end() 中的恢复
        boolean applyIsolationBeforeThrow;              // 模拟驱动先应用隔离级别再抛出异常

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
                int newIsolation = (Integer) args[0];
                if (setTransactionIsolationFailure != null && !setTransactionIsolationFailureThrown
                        && setTransactionIsolationFailureValue != null
                        && setTransactionIsolationFailureValue == newIsolation) {
                    setTransactionIsolationFailureThrown = true;
                    if (applyIsolationBeforeThrow) {
                        isolation = newIsolation;   // 驱动先应用隔离级别再抛出异常
                    }
                    throw setTransactionIsolationFailure;
                }
                isolation = newIsolation;
                return null;
            }
            if ("getAutoCommit".equals(name)) {
                return autoCommit;
            }
            if ("setAutoCommit".equals(name)) {
                setAutoCommitCount++;
                boolean newValue = (Boolean) args[0];
                if (autoCommit && !newValue) {
                    transactionResolved = false;
                }
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
                if (commitFailure != null) {
                    throw commitFailure;
                }
                transactionResolved = true;
                return null;
            }
            if ("close".equals(name)) {
                closeCount++;
                if (closeFailure != null) {
                    throw closeFailure;
                }
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

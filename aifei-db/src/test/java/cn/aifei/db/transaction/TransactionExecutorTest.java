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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TransactionExecutorTest {

    private static final AtomicInteger CONFIG_SEQUENCE = new AtomicInteger();
    private static LogFactory originalLogFactory;

    @BeforeClass
    public static void installNoOpLogFactory() {
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
    public void successfulTransactionCommitsThenRestoresAndClosesConnection() {
        Fixture fixture = fixture();
        fixture.connection.isolation = Connection.TRANSACTION_READ_COMMITTED;

        String result = fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
            assertTrue(tx.canCommit());
            assertTrue(fixture.config.getTransactionKit().isInTransaction());
            assertSame(fixture.connectionProxy, tx.getConnection());
            assertSame(fixture.connectionProxy, fixture.config.getConnection());
            return "committed";
        });

        assertEquals("committed", result);
        assertEquals(1, fixture.connection.commitCount);
        assertEquals(0, fixture.connection.rollbackCount);
        assertTrue(fixture.connection.autoCommit);
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, fixture.connection.isolation);
        assertEquals(1, fixture.connection.closeCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
        assertEquals(Arrays.asList(
                "getTransactionIsolation",
                "setTransactionIsolation:4",
                "getAutoCommit",
                "setAutoCommit:false",
                "commit",
                "setAutoCommit:true",
                "setTransactionIsolation:2",
                "close"), fixture.connection.events);
    }

    @Test
    public void explicitRollbackIsStickyAndSkipsBeforeCommitCallback() {
        Fixture fixture = fixture();
        AtomicBoolean beforeCommitInvoked = new AtomicBoolean();
        AtomicBoolean commitSuccessInvoked = new AtomicBoolean();
        fixture.config.setOnBeforeTransactionCommit((tx, value) -> beforeCommitInvoked.set(true));

        String result = fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
            tx.onCommitSuccess(() -> commitSuccessInvoked.set(true));
            assertFalse(tx.rollbackIf(false));
            assertTrue(tx.canCommit());
            assertTrue(tx.rollbackIf(true));
            assertFalse(tx.canCommit());
            tx.rollback();
            return "rolled-back";
        });

        assertEquals("rolled-back", result);
        assertFalse(beforeCommitInvoked.get());
        assertFalse(commitSuccessInvoked.get());
        assertEquals(0, fixture.connection.commitCount);
        assertEquals(1, fixture.connection.rollbackCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void rollbackDecisionControlsCommitDirection() {
        Fixture rollbackFixture = fixture();
        Decision rollbackResult = rollbackFixture.executor.execute(
                rollbackFixture.config, Isolation.REPEATABLE_READ, tx -> new Decision(true));

        assertTrue(rollbackResult.shouldRollback());
        assertEquals(0, rollbackFixture.connection.commitCount);
        assertEquals(1, rollbackFixture.connection.rollbackCount);

        Fixture commitFixture = fixture();
        Decision commitResult = commitFixture.executor.execute(
                commitFixture.config, Isolation.REPEATABLE_READ, tx -> new Decision(false));

        assertFalse(commitResult.shouldRollback());
        assertEquals(1, commitFixture.connection.commitCount);
        assertEquals(0, commitFixture.connection.rollbackCount);
    }

    @Test
    public void beforeCommitRunsForEachNestedLevelAndMayRollbackOuterTransaction() {
        Fixture fixture = fixture();
        List<String> values = new ArrayList<>();
        fixture.config.setOnBeforeTransactionCommit((tx, value) -> {
            values.add((String) value);
            if ("outer".equals(value)) {
                tx.rollback();
            }
        });

        String result = fixture.executor.execute(fixture.config, Isolation.READ_COMMITTED, outer -> {
            String innerResult = fixture.executor.execute(
                    fixture.config, Isolation.READ_COMMITTED, inner -> "inner");
            assertEquals("inner", innerResult);
            return "outer";
        });

        assertEquals("outer", result);
        assertEquals(Arrays.asList("inner", "outer"), values);
        assertEquals(0, fixture.connection.commitCount);
        assertEquals(1, fixture.connection.rollbackCount);
    }

    @Test
    public void nestedTransactionsShareConnectionOnlyUpgradeIsolationAndRestoreOriginalValue() {
        Fixture fixture = fixture();
        fixture.connection.isolation = Connection.TRANSACTION_READ_COMMITTED;

        fixture.executor.execute(fixture.config, Isolation.READ_COMMITTED, outer -> {
            assertSame(outer, fixture.config.getTransactionKit().getTransaction());
            fixture.executor.execute(fixture.config, Isolation.READ_UNCOMMITTED, inner -> {
                assertSame(outer, inner);
                assertSame(fixture.connectionProxy, inner.getConnection());
                return null;
            });
            fixture.executor.execute(fixture.config, Isolation.SERIALIZABLE, inner -> null);
            return null;
        });

        assertEquals(1, fixture.dataSource.getConnectionCount);
        assertEquals(1, fixture.connection.commitCount);
        assertEquals(0, fixture.connection.rollbackCount);
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, fixture.connection.isolation);
        assertEquals(Arrays.asList(8, 2), fixture.connection.isolationChanges);
    }

    @Test
    public void caughtNestedExceptionStillMarksOuterTransactionForRollback() {
        Fixture fixture = fixture();
        RuntimeException expected = new RuntimeException("nested");

        String result = fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, outer -> {
            try {
                fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, inner -> {
                    throw expected;
                });
                fail("nested exception expected");
            } catch (RuntimeException actual) {
                assertSame(expected, actual);
            }
            assertFalse(outer.canCommit());
            return "continued";
        });

        assertEquals("continued", result);
        assertEquals(0, fixture.connection.commitCount);
        assertEquals(1, fixture.connection.rollbackCount);
    }

    @Test
    public void nestedAndOuterExceptionCallbacksRemainScopedToTheirOwnLevels() {
        Fixture fixture = fixture();
        Exception expected = new Exception("nested");
        List<String> callbacks = new ArrayList<>();

        String result = fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, outer -> {
            outer.onException(exception -> {
                callbacks.add("outer");
                assertTrue(exception instanceof AifeiDbException);
                assertSame(expected, exception.getCause());
                return "fallback";
            });
            return fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, inner -> {
                inner.onException(exception -> {
                    callbacks.add("inner");
                    assertSame(expected, exception);
                    return "ignored";
                });
                throw expected;
            });
        });

        assertEquals("fallback", result);
        assertEquals(Arrays.asList("inner", "outer"), callbacks);
        assertEquals(1, fixture.connection.rollbackCount);
        assertEquals(0, fixture.connection.commitCount);
    }

    @Test
    public void runtimeExceptionRollsBackAndKeepsIdentity() {
        Fixture fixture = fixture();
        RuntimeException expected = new RuntimeException("atom");

        try {
            fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
                throw expected;
            });
            fail("runtime exception expected");
        } catch (RuntimeException actual) {
            assertSame(expected, actual);
        }

        assertEquals(1, fixture.connection.rollbackCount);
        assertEquals(0, fixture.connection.commitCount);
        assertTrue(fixture.connection.autoCommit);
        assertEquals(1, fixture.connection.closeCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void checkedExceptionIsWrappedAfterRollback() {
        Fixture fixture = fixture();
        Exception expected = new Exception("atom");

        try {
            fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
                throw expected;
            });
            fail("AifeiDbException expected");
        } catch (AifeiDbException actual) {
            assertSame(expected, actual.getCause());
        }

        assertEquals(1, fixture.connection.rollbackCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void errorRollsBackAndKeepsIdentity() {
        Fixture fixture = fixture();
        AssertionError expected = new AssertionError("atom");

        try {
            fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
                throw expected;
            });
            fail("error expected");
        } catch (AssertionError actual) {
            assertSame(expected, actual);
        }

        assertEquals(1, fixture.connection.rollbackCount);
        assertTrue(fixture.connection.autoCommit);
        assertEquals(1, fixture.connection.closeCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void connectionAcquisitionFailureNeverInvokesExceptionCallback() {
        Fixture fixture = fixture();
        SQLException expected = new SQLException("getConnection");
        fixture.dataSource.connectionFailure = expected;
        AtomicBoolean callbackInvoked = new AtomicBoolean();
        fixture.config.setOnTransactionException(exception -> {
            callbackInvoked.set(true);
            return "fallback";
        });

        try {
            fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> "unused");
            fail("AifeiDbException expected");
        } catch (AifeiDbException actual) {
            assertSame(expected, actual.getCause());
        }

        assertFalse(callbackInvoked.get());
        assertEquals(1, fixture.dataSource.getConnectionCount);
        assertEquals(0, fixture.connection.closeCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void beginFailureNeverInvokesCallbackOrAtom() {
        Fixture fixture = fixture();
        SQLException expected = new SQLException("getTransactionIsolation");
        fixture.connection.getIsolationFailure = expected;
        AtomicBoolean callbackInvoked = new AtomicBoolean();
        AtomicBoolean atomInvoked = new AtomicBoolean();
        fixture.config.setOnTransactionException(exception -> {
            callbackInvoked.set(true);
            return "fallback";
        });

        try {
            fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
                atomInvoked.set(true);
                return "unused";
            });
            fail("AifeiDbException expected");
        } catch (AifeiDbException actual) {
            assertSame(expected, actual.getCause());
        }

        assertFalse(callbackInvoked.get());
        assertFalse(atomInvoked.get());
        assertEquals(0, fixture.connection.rollbackCount);
        assertEquals(1, fixture.connection.closeCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void partiallyAppliedBeginStateIsRestoredWithoutInvokingCallback() {
        Fixture fixture = fixture();
        fixture.connection.isolation = Connection.TRANSACTION_READ_COMMITTED;
        SQLException expected = new SQLException("setAutoCommit(false)");
        fixture.connection.setAutoCommitFalseFailure = expected;
        AtomicBoolean callbackInvoked = new AtomicBoolean();
        fixture.config.setOnTransactionException(exception -> {
            callbackInvoked.set(true);
            return "fallback";
        });

        try {
            fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> "unused");
            fail("AifeiDbException expected");
        } catch (AifeiDbException actual) {
            assertSame(expected, actual.getCause());
        }

        assertFalse(callbackInvoked.get());
        assertEquals(0, fixture.connection.rollbackCount);
        assertTrue(fixture.connection.autoCommit);
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, fixture.connection.isolation);
        assertEquals(Arrays.asList(false, true), fixture.connection.autoCommitChanges);
        assertEquals(Arrays.asList(4, 2), fixture.connection.isolationChanges);
        assertEquals(1, fixture.connection.closeCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void localExceptionCallbackOverridesGlobalCallbackAndRunsAfterRollback() {
        Fixture fixture = fixture();
        RuntimeException expected = new RuntimeException("atom");
        AtomicBoolean globalInvoked = new AtomicBoolean();
        fixture.config.setOnTransactionException(exception -> {
            globalInvoked.set(true);
            return "global";
        });

        String result = fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
            tx.onException(exception -> {
                assertSame(expected, exception);
                assertEquals(1, fixture.connection.rollbackCount);
                assertFalse(fixture.connection.autoCommit);
                assertTrue(fixture.config.getTransactionKit().isInTransaction());
                return "local";
            });
            throw expected;
        });

        assertEquals("local", result);
        assertFalse(globalInvoked.get());
        assertTrue(fixture.connection.autoCommit);
        assertEquals(1, fixture.connection.closeCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void exceptionCallbackCannotAccessCurrentTransactionConnection() {
        Fixture fixture = fixture();
        AtomicReference<Transaction<?>> transactionRef = new AtomicReference<>();

        try {
            fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
                transactionRef.set(tx);
                tx.onException(exception -> {
                    tx.getConnection();
                    return "unreachable";
                });
                throw new Exception("atom");
            });
            fail("AifeiDbException expected");
        } catch (AifeiDbException actual) {
            assertEquals("Database operations and transactions are not allowed in an onException callback.",
                    actual.getMessage());
        }

        assertNotNull(transactionRef.get());
        // executeOnException 的 finally 必须清除保护标记，不能永久污染 Transaction 对象。
        assertSame(fixture.connectionProxy, transactionRef.get().getConnection());
        assertEquals(1, fixture.connection.rollbackCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void globalExceptionCallbackCannotUseSameConfigDatabaseEntry() {
        Fixture fixture = fixture();
        fixture.config.setOnTransactionException(exception -> {
            fixture.config.getConnection();
            return "unreachable";
        });

        try {
            fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
                throw new Exception("atom");
            });
            fail("AifeiDbException expected");
        } catch (AifeiDbException actual) {
            assertTrue(rootCause(actual) instanceof AifeiDbException);
            assertEquals("Database operations and transactions are not allowed in an onException callback.",
                    rootCause(actual).getMessage());
        }

        assertEquals(1, fixture.dataSource.getConnectionCount);
        assertEquals(1, fixture.connection.rollbackCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void exceptionCallbackCannotOpenNestedTransactionOnSameConfig() {
        Fixture fixture = fixture();
        AtomicBoolean nestedAtomInvoked = new AtomicBoolean();

        try {
            fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
                tx.onException(exception -> fixture.executor.execute(
                        fixture.config, Isolation.REPEATABLE_READ, nested -> {
                            nestedAtomInvoked.set(true);
                            return "unreachable";
                        }));
                throw new Exception("atom");
            });
            fail("AifeiDbException expected");
        } catch (AifeiDbException actual) {
            assertEquals("Database operations and transactions are not allowed in an onException callback.",
                    actual.getMessage());
        }

        assertFalse(nestedAtomInvoked.get());
        assertEquals(1, fixture.dataSource.getConnectionCount);
        assertEquals(1, fixture.connection.rollbackCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void exceptionCallbackMayUseDifferentConfigAndDirectDataSource() {
        Fixture fixture = fixture();
        Fixture other = fixture();
        fixture.config.setOnTransactionException(exception -> {
            assertSame(other.connectionProxy, other.config.getConnection());
            try {
                assertSame(fixture.connectionProxy, fixture.config.getDataSource().getConnection());
            } catch (SQLException e) {
                throw new AssertionError(e);
            }
            return "fallback";
        });

        String result = fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
            throw new Exception("atom");
        });

        assertEquals("fallback", result);
        assertEquals(1, fixture.connection.rollbackCount);
        assertEquals(2, fixture.dataSource.getConnectionCount);
        assertEquals(1, other.dataSource.getConnectionCount);
        assertEquals(0, other.connection.commitCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
        assertFalse(other.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void nestedCallbackGuardFailureDoesNotMaskOriginalException() {
        Fixture fixture = fixture();
        Exception expected = new Exception("nested");
        AtomicBoolean nestedAtomInvoked = new AtomicBoolean();

        try {
            fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, outer ->
                    fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, inner -> {
                        inner.onException(exception -> fixture.executor.execute(
                                fixture.config, Isolation.REPEATABLE_READ, nested -> {
                                    nestedAtomInvoked.set(true);
                                    return "unreachable";
                                }));
                        throw expected;
                    }));
            fail("AifeiDbException expected");
        } catch (AifeiDbException actual) {
            assertSame(expected, actual.getCause());
        }

        assertFalse(nestedAtomInvoked.get());
        assertEquals(1, fixture.connection.rollbackCount);
        assertEquals(0, fixture.connection.commitCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void beforeCommitFailureRollsBackAndMayBeConvertedByExceptionCallback() {
        Fixture fixture = fixture();
        RuntimeException expected = new RuntimeException("beforeCommit");
        AtomicReference<Exception> callbackArgument = new AtomicReference<>();
        fixture.config.setOnBeforeTransactionCommit((tx, value) -> {
            throw expected;
        });
        fixture.config.setOnTransactionException(exception -> {
            callbackArgument.set(exception);
            return "fallback";
        });

        String result = fixture.executor.execute(
                fixture.config, Isolation.REPEATABLE_READ, tx -> "atom-result");

        assertEquals("fallback", result);
        assertSame(expected, callbackArgument.get());
        assertEquals(0, fixture.connection.commitCount);
        assertEquals(1, fixture.connection.rollbackCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void commitFailureAttemptsRollbackAndMayBeConvertedByExceptionCallback() {
        Fixture fixture = fixture();
        SQLException expected = new SQLException("commit");
        fixture.connection.commitFailure = expected;
        AtomicReference<Exception> callbackArgument = new AtomicReference<>();
        AtomicBoolean commitSuccessInvoked = new AtomicBoolean();

        String result = fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
            tx.onCommitSuccess(() -> commitSuccessInvoked.set(true));
            tx.onException(exception -> {
                callbackArgument.set(exception);
                return "fallback";
            });
            return "unused";
        });

        assertEquals("fallback", result);
        assertSame(expected, callbackArgument.get());
        assertEquals(1, fixture.connection.commitCount);
        assertEquals(1, fixture.connection.rollbackCount);
        assertFalse(commitSuccessInvoked.get());
        assertTrue(fixture.connection.autoCommit);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void rollbackExceptionDoesNotReplaceOriginalAtomExceptionAndSkipsStateRestoration() {
        Fixture fixture = fixture();
        fixture.connection.isolation = Connection.TRANSACTION_READ_COMMITTED;
        SQLException rollbackFailure = new SQLException("rollback");
        fixture.connection.rollbackFailure = rollbackFailure;
        Exception atomFailure = new Exception("atom");
        AtomicReference<Exception> callbackArgument = new AtomicReference<>();
        fixture.config.setOnTransactionException(exception -> {
            callbackArgument.set(exception);
            return "fallback";
        });

        String result = fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
            throw atomFailure;
        });

        assertEquals("fallback", result);
        assertSame(atomFailure, callbackArgument.get());
        assertEquals(1, fixture.connection.rollbackCount);
        // ROLLING_BACK 表示结果未确认，此时恢复 autoCommit 可能隐式提交本应回滚的事务。
        assertFalse(fixture.connection.autoCommit);
        assertEquals(Connection.TRANSACTION_REPEATABLE_READ, fixture.connection.isolation);
        assertEquals(Arrays.asList(4), fixture.connection.isolationChanges);
        assertEquals(0, fixture.connection.implicitCommitCount);
        assertEquals(1, fixture.connection.closeCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void directRollbackFailureIsTheExceptionSeenByCallback() {
        Fixture fixture = fixture();
        SQLException expected = new SQLException("rollback");
        fixture.connection.rollbackFailure = expected;
        AtomicReference<Exception> callbackArgument = new AtomicReference<>();
        fixture.config.setOnTransactionException(exception -> {
            callbackArgument.set(exception);
            return "fallback";
        });

        String result = fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
            tx.rollback();
            return "unused";
        });

        assertEquals("fallback", result);
        assertSame(expected, callbackArgument.get());
        assertEquals(1, fixture.connection.rollbackCount);
        assertFalse(fixture.connection.autoCommit);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void rollbackErrorPreemptsExceptionCallbackAndKeepsIdentity() {
        Fixture fixture = fixture();
        AssertionError expected = new AssertionError("rollback");
        fixture.connection.rollbackFailure = expected;
        AtomicBoolean callbackInvoked = new AtomicBoolean();
        fixture.config.setOnTransactionException(exception -> {
            callbackInvoked.set(true);
            return "fallback";
        });

        try {
            fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
                throw new Exception("atom");
            });
            fail("rollback error expected");
        } catch (AssertionError actual) {
            assertSame(expected, actual);
        }

        assertFalse(callbackInvoked.get());
        assertEquals(1, fixture.connection.rollbackCount);
        assertFalse(fixture.connection.autoCommit);
        assertEquals(1, fixture.connection.closeCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void commitSuccessCallbacksRunAfterCleanupInReverseOrderAndIgnoreExceptions() {
        Fixture fixture = fixture();
        List<String> callbacks = new ArrayList<>();

        String result = fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
            tx.onCommitSuccess(() -> {
                callbacks.add("first");
                assertTrue(fixture.connection.autoCommit);
                assertEquals(1, fixture.connection.closeCount);
                assertFalse(fixture.config.getTransactionKit().isInTransaction());
            });
            tx.onCommitSuccess(() -> {
                callbacks.add("second");
                throw new RuntimeException("ignored");
            });
            tx.onCommitSuccess(() -> callbacks.add("third"));
            tx.onCommitSuccess(null);
            return "result";
        });

        assertEquals("result", result);
        assertEquals(Arrays.asList("third", "second", "first"), callbacks);
        assertEquals(1, fixture.connection.commitCount);
        assertEquals(0, fixture.connection.rollbackCount);
    }

    @Test
    public void commitSuccessCallbackMayStartFreshTopLevelTransaction() {
        Fixture fixture = fixture();
        AtomicBoolean nestedAtomInvoked = new AtomicBoolean();

        String result = fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
            tx.onCommitSuccess(() -> {
                String nestedResult = fixture.executor.execute(
                        fixture.config, Isolation.REPEATABLE_READ, nested -> {
                            nestedAtomInvoked.set(true);
                            return "fresh";
                        });
                assertEquals("fresh", nestedResult);
            });
            return "outer";
        });

        assertEquals("outer", result);
        assertTrue(nestedAtomInvoked.get());
        assertEquals(2, fixture.dataSource.getConnectionCount);
        assertEquals(2, fixture.connection.commitCount);
        assertEquals(2, fixture.connection.closeCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void commitSuccessCallbackStillRunsAfterConnectionCloseFailure() {
        Fixture fixture = fixture();
        fixture.connection.closeFailure = new SQLException("close");
        AtomicBoolean callbackInvoked = new AtomicBoolean();

        String result = fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
            tx.onCommitSuccess(() -> {
                callbackInvoked.set(true);
                assertEquals(1, fixture.connection.closeCount);
                assertFalse(fixture.config.getTransactionKit().isInTransaction());
            });
            return "result";
        });

        assertEquals("result", result);
        assertTrue(callbackInvoked.get());
        assertEquals(1, fixture.connection.commitCount);
        assertEquals(1, fixture.connection.closeCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void commitSuccessCallbackIsNotRunForBusinessException() {
        Fixture fixture = fixture();
        AtomicBoolean callbackInvoked = new AtomicBoolean();

        try {
            fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
                tx.onCommitSuccess(() -> callbackInvoked.set(true));
                throw new Exception("atom");
            });
            fail("AifeiDbException expected");
        } catch (AifeiDbException expected) {
            assertEquals("atom", expected.getCause().getMessage());
        }

        assertFalse(callbackInvoked.get());
        assertEquals(0, fixture.connection.commitCount);
        assertEquals(1, fixture.connection.rollbackCount);
    }

    @Test
    public void commitSuccessCallbackErrorPropagatesAfterSuccessfulCommitAndCleanup() {
        Fixture fixture = fixture();
        AssertionError expected = new AssertionError("callback");

        try {
            fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, tx -> {
                tx.onCommitSuccess(() -> {
                    throw expected;
                });
                return "unused";
            });
            fail("callback error expected");
        } catch (AssertionError actual) {
            assertSame(expected, actual);
        }

        assertEquals(1, fixture.connection.commitCount);
        assertEquals(0, fixture.connection.rollbackCount);
        assertEquals(1, fixture.connection.closeCount);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    @Test
    public void appliedThenFailedNestedIsolationUpgradeIsRestoredAtTransactionEnd() {
        Fixture fixture = fixture();
        fixture.connection.isolation = Connection.TRANSACTION_REPEATABLE_READ;
        fixture.connection.setIsolationFailureValue = Connection.TRANSACTION_SERIALIZABLE;
        fixture.connection.setIsolationFailure = new SQLException("setTransactionIsolation");
        fixture.connection.applyIsolationBeforeFailure = true;

        String result = fixture.executor.execute(fixture.config, Isolation.REPEATABLE_READ, outer -> {
            try {
                fixture.executor.execute(fixture.config, Isolation.SERIALIZABLE, inner -> "unused");
                fail("AifeiDbException expected");
            } catch (AifeiDbException actual) {
                assertSame(fixture.connection.setIsolationFailure, actual.getCause());
            }
            assertFalse(outer.canCommit());
            return "continued";
        });

        assertEquals("continued", result);
        assertEquals(1, fixture.connection.rollbackCount);
        assertEquals(0, fixture.connection.commitCount);
        assertEquals(Arrays.asList(8, 4), fixture.connection.isolationChanges);
        assertEquals(Connection.TRANSACTION_REPEATABLE_READ, fixture.connection.isolation);
        assertFalse(fixture.config.getTransactionKit().isInTransaction());
    }

    private static Fixture fixture() {
        ConnectionRecorder connection = new ConnectionRecorder();
        Connection connectionProxy = (Connection) Proxy.newProxyInstance(
                TransactionExecutorTest.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                connection);
        RecordingDataSource dataSource = new RecordingDataSource(connectionProxy);
        DbConfig config = new DbConfig(
                "transaction-test-" + CONFIG_SEQUENCE.incrementAndGet(), dataSource);
        return new Fixture(config, dataSource, connection, connectionProxy);
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable result = throwable;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class Fixture {

        final DbConfig config;
        final RecordingDataSource dataSource;
        final ConnectionRecorder connection;
        final Connection connectionProxy;
        final TransactionExecutor executor = new TransactionExecutor();

        Fixture(DbConfig config, RecordingDataSource dataSource,
                ConnectionRecorder connection, Connection connectionProxy) {
            this.config = config;
            this.dataSource = dataSource;
            this.connection = connection;
            this.connectionProxy = connectionProxy;
        }
    }

    private static final class Decision implements RollbackDecision {

        private final boolean rollback;

        Decision(boolean rollback) {
            this.rollback = rollback;
        }

        @Override
        public boolean shouldRollback() {
            return rollback;
        }
    }

    private static final class ConnectionRecorder implements InvocationHandler {

        final List<String> events = new ArrayList<>();
        final List<Integer> isolationChanges = new ArrayList<>();
        final List<Boolean> autoCommitChanges = new ArrayList<>();

        boolean autoCommit = true;
        boolean transactionResolved = true;
        int isolation = Connection.TRANSACTION_REPEATABLE_READ;
        int commitCount;
        int rollbackCount;
        int closeCount;
        int implicitCommitCount;

        Throwable getIsolationFailure;
        Throwable setIsolationFailure;
        Integer setIsolationFailureValue;
        boolean setIsolationFailureThrown;
        boolean applyIsolationBeforeFailure;
        Throwable setAutoCommitFalseFailure;
        boolean setAutoCommitFalseFailureThrown;
        Throwable commitFailure;
        Throwable rollbackFailure;
        Throwable closeFailure;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if ("getTransactionIsolation".equals(methodName)) {
                events.add("getTransactionIsolation");
                if (getIsolationFailure != null) {
                    throw getIsolationFailure;
                }
                return isolation;
            }
            if ("setTransactionIsolation".equals(methodName)) {
                int newIsolation = (Integer) args[0];
                events.add("setTransactionIsolation:" + newIsolation);
                isolationChanges.add(newIsolation);
                if (setIsolationFailure != null && !setIsolationFailureThrown
                        && setIsolationFailureValue != null
                        && setIsolationFailureValue == newIsolation) {
                    setIsolationFailureThrown = true;
                    if (applyIsolationBeforeFailure) {
                        isolation = newIsolation;
                    }
                    throw setIsolationFailure;
                }
                isolation = newIsolation;
                return null;
            }
            if ("getAutoCommit".equals(methodName)) {
                events.add("getAutoCommit");
                return autoCommit;
            }
            if ("setAutoCommit".equals(methodName)) {
                boolean newAutoCommit = (Boolean) args[0];
                events.add("setAutoCommit:" + newAutoCommit);
                autoCommitChanges.add(newAutoCommit);
                applyAutoCommit(newAutoCommit);
                if (!newAutoCommit && setAutoCommitFalseFailure != null
                        && !setAutoCommitFalseFailureThrown) {
                    setAutoCommitFalseFailureThrown = true;
                    throw setAutoCommitFalseFailure;
                }
                return null;
            }
            if ("commit".equals(methodName)) {
                events.add("commit");
                commitCount++;
                if (commitFailure != null) {
                    throw commitFailure;
                }
                transactionResolved = true;
                return null;
            }
            if ("rollback".equals(methodName)) {
                events.add("rollback");
                rollbackCount++;
                if (rollbackFailure != null) {
                    throw rollbackFailure;
                }
                transactionResolved = true;
                return null;
            }
            if ("close".equals(methodName)) {
                events.add("close");
                closeCount++;
                if (closeFailure != null) {
                    throw closeFailure;
                }
                return null;
            }
            if ("isWrapperFor".equals(methodName)) {
                return false;
            }
            if ("unwrap".equals(methodName)) {
                throw new SQLException("not a wrapper");
            }
            return defaultValue(method.getReturnType());
        }

        private void applyAutoCommit(boolean newAutoCommit) {
            if (autoCommit && !newAutoCommit) {
                transactionResolved = false;
            } else if (!autoCommit && newAutoCommit && !transactionResolved) {
                implicitCommitCount++;
            }
            autoCommit = newAutoCommit;
        }
    }

    private static final class RecordingDataSource implements DataSource {

        private final Connection connection;
        int getConnectionCount;
        SQLException connectionFailure;

        RecordingDataSource(Connection connection) {
            this.connection = connection;
        }

        @Override
        public Connection getConnection() throws SQLException {
            getConnectionCount++;
            if (connectionFailure != null) {
                throw connectionFailure;
            }
            return connection;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
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
            throw new SQLException("not a wrapper");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }
}

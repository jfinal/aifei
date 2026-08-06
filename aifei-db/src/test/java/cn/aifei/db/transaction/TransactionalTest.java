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

import cn.aifei.aop.Interceptor;
import cn.aifei.aop.Invocation;
import cn.aifei.db.core.DbConfig;
import cn.aifei.db.core.DbKit;
import cn.aifei.log.Log;
import cn.aifei.log.LogFactory;
import cn.aifei.log.LogKit;
import cn.aifei.proxy.Callback;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class TransactionalTest {

    private static LogFactory originalLogFactory;
    private ConnectionRecorder connection;

    @BeforeClass
    public static void installNoOpLogFactory() {
        originalLogFactory = LogKit.get().getLogFactory();
        Log noOpLog = (Log) Proxy.newProxyInstance(
                TransactionalTest.class.getClassLoader(),
                new Class<?>[]{Log.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
        LogFactory noOpLogFactory = (LogFactory) Proxy.newProxyInstance(
                TransactionalTest.class.getClassLoader(),
                new Class<?>[]{LogFactory.class},
                (proxy, method, args) -> noOpLog);
        LogKit.get().setLogFactory(noOpLogFactory);
    }

    @AfterClass
    public static void restoreLogFactory() {
        LogKit.get().setLogFactory(originalLogFactory);
    }

    @Before
    public void setUp() {
        connection = new ConnectionRecorder();
        Connection connectionProxy = (Connection) Proxy.newProxyInstance(
                TransactionalTest.class.getClassLoader(),
                new Class<?>[]{Connection.class}, connection);
        DataSource dataSource = (DataSource) Proxy.newProxyInstance(
                TransactionalTest.class.getClassLoader(),
                new Class<?>[]{DataSource.class}, new DataSourceHandler(connectionProxy));

        DbKit.setDbUseThreadLocalConfig(true);
        DbKit.setThreadLocalConfig(new DbConfig("transactional-test", dataSource));
        Transactional.THREAD_LOCAL.remove();
    }

    @After
    public void tearDown() {
        Transactional.THREAD_LOCAL.remove();
        DbKit.removeThreadLocalConfig();
        DbKit.setDbUseThreadLocalConfig(false);
    }

    @Test
    public void transactionArgumentInjectsCurrentTransactionAndMayRollback() throws Exception {
        TransactionArgument argument = new TransactionArgument();
        AtomicReference<Transaction<?>> injected = new AtomicReference<>();
        Invocation invocation = invocation(args -> {
            injected.set(argument.getValue(null, null));
            assertSame(Transactional.getTransaction(), injected.get());
            injected.get().rollback();
            return "rolled-back";
        });

        new Transactional().intercept(invocation);

        assertEquals("rolled-back", invocation.getReturnValue());
        assertEquals(0, connection.commitCount);
        assertEquals(1, connection.rollbackCount);
        assertNull(Transactional.getTransaction());
    }

    @Test
    public void exceptionCallbackReturnValueIsWrittenBackToInvocation() throws Exception {
        RuntimeException expected = new RuntimeException("action");
        AtomicReference<Exception> callbackArgument = new AtomicReference<>();
        Invocation invocation = invocation(args -> {
            Transaction<String> transaction = currentTransaction();
            transaction.onException(exception -> {
                callbackArgument.set(exception);
                return "fallback";
            });
            throw expected;
        });

        new Transactional().intercept(invocation);

        assertEquals("fallback", invocation.getReturnValue());
        assertSame(expected, callbackArgument.get());
        assertEquals(0, connection.commitCount);
        assertEquals(1, connection.rollbackCount);
        assertNull(Transactional.getTransaction());
    }

    @Test
    public void nestedTransactionalInvocationRestoresOuterTransaction() throws Exception {
        AtomicReference<Transaction<?>> outerTransaction = new AtomicReference<>();
        Invocation inner = invocation(args -> {
            assertSame(outerTransaction.get(), Transactional.getTransaction());
            return "inner";
        });
        Invocation outer = invocation(args -> {
            outerTransaction.set(Transactional.getTransaction());
            new Transactional().intercept(inner);
            assertSame(outerTransaction.get(), Transactional.getTransaction());
            return "outer";
        });

        new Transactional().intercept(outer);

        assertEquals("inner", inner.getReturnValue());
        assertEquals("outer", outer.getReturnValue());
        assertEquals(1, connection.commitCount);
        assertEquals(0, connection.rollbackCount);
        assertNull(Transactional.getTransaction());
    }

    @Test
    public void transactionArgumentFailsFastOutsideTransactionalInterceptor() {
        try {
            new TransactionArgument().getValue(null, null);
            fail("IllegalStateException expected");
        } catch (IllegalStateException actual) {
            assertEquals("TransactionArgument requires Transactional interceptor.", actual.getMessage());
        }
    }

    private static Invocation invocation(Callback callback) throws NoSuchMethodException {
        Method method = Object.class.getMethod("toString");
        return new Invocation(null, method, new Object[0], new Interceptor[0], callback);
    }

    @SuppressWarnings("unchecked")
    private static <T> Transaction<T> currentTransaction() {
        return (Transaction<T>) Transactional.getTransaction();
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

    private static final class ConnectionRecorder implements InvocationHandler {

        boolean autoCommit = true;
        int isolation = Connection.TRANSACTION_READ_COMMITTED;
        int commitCount;
        int rollbackCount;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws SQLException {
            String methodName = method.getName();
            if ("getTransactionIsolation".equals(methodName)) {
                return isolation;
            }
            if ("setTransactionIsolation".equals(methodName)) {
                isolation = (Integer) args[0];
                return null;
            }
            if ("getAutoCommit".equals(methodName)) {
                return autoCommit;
            }
            if ("setAutoCommit".equals(methodName)) {
                autoCommit = (Boolean) args[0];
                return null;
            }
            if ("commit".equals(methodName)) {
                commitCount++;
                return null;
            }
            if ("rollback".equals(methodName)) {
                rollbackCount++;
                return null;
            }
            if ("unwrap".equals(methodName)) {
                throw new SQLException("not a wrapper");
            }
            if ("isWrapperFor".equals(methodName)) {
                return false;
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static final class DataSourceHandler implements InvocationHandler {

        private final Connection connection;

        DataSourceHandler(Connection connection) {
            this.connection = connection;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws SQLException {
            String methodName = method.getName();
            if ("getConnection".equals(methodName)) {
                return connection;
            }
            if ("getParentLogger".equals(methodName)) {
                return Logger.getGlobal();
            }
            if ("unwrap".equals(methodName)) {
                throw new SQLException("not a wrapper");
            }
            if ("isWrapperFor".equals(methodName)) {
                return false;
            }
            return defaultValue(method.getReturnType());
        }
    }
}

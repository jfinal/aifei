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

package cn.aifei.proxy;

import cn.aifei.aop.Before;
import cn.aifei.aop.Interceptor;
import cn.aifei.aop.Invocation;
import cn.aifei.proxy.cglib.CglibProxyFactory;
import cn.aifei.proxy.javassist.JavassistProxyFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class ProxyExceptionTest {

    static final CheckedFailure CHECKED_FAILURE = new CheckedFailure();
    static final RuntimeException RUNTIME_FAILURE = new RuntimeException();
    static final Error ERROR = new AssertionError();
    static final DirectThrowable DIRECT_THROWABLE = new DirectThrowable();
    static final InvocationTargetException USER_INVOCATION_TARGET_EXCEPTION =
            new InvocationTargetException(new IllegalStateException());

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> proxyFactories() {
        return Arrays.asList(new Object[][] {
                {"cglib", new CglibProxyFactory()},
                {"javassist", new JavassistProxyFactory()}
        });
    }

    private final ProxyFactory factory;

    public ProxyExceptionTest(String name, ProxyFactory factory) {
        this.factory = factory;
    }

    @Test
    public void exceptionsKeepTheSameContractWithAndWithoutInterceptors() {
        ProceedInterceptor.invocationCount = 0;

        assertContract(factory.get(PlainFailureService.class));
        assertEquals(0, ProceedInterceptor.invocationCount);

        assertContract(factory.get(InterceptedFailureService.class));
        assertEquals(5, ProceedInterceptor.invocationCount);
    }

    private void assertContract(FailureService service) {
        assertSameFailure(CHECKED_FAILURE, service::checked);
        assertSameFailure(RUNTIME_FAILURE, service::runtime);
        assertSameFailure(ERROR, service::error);
        assertWrappedFailure(DIRECT_THROWABLE, service::directThrowable);
        assertSameFailure(USER_INVOCATION_TARGET_EXCEPTION, service::invocationTargetException);
    }

    private void assertSameFailure(Throwable expected, ThrowingCall call) {
        try {
            call.run();
            fail("Expected " + expected.getClass().getName());
        } catch (Throwable actual) {
            assertSame(expected, actual);
        }
    }

    private void assertWrappedFailure(Throwable expectedCause, ThrowingCall call) {
        try {
            call.run();
            fail("Expected RuntimeException");
        } catch (RuntimeException actual) {
            assertSame(expectedCause, actual.getCause());
        } catch (Throwable actual) {
            fail("Expected RuntimeException but got " + actual.getClass().getName());
        }
    }

    interface FailureService {
        void checked() throws CheckedFailure;
        void runtime();
        void error();
        void directThrowable() throws Throwable;
        void invocationTargetException() throws InvocationTargetException;
    }

    public static class PlainFailureService implements FailureService {
        @Override
        public void checked() throws CheckedFailure {
            throw CHECKED_FAILURE;
        }

        @Override
        public void runtime() {
            throw RUNTIME_FAILURE;
        }

        @Override
        public void error() {
            throw ERROR;
        }

        @Override
        public void directThrowable() throws Throwable {
            throw DIRECT_THROWABLE;
        }

        @Override
        public void invocationTargetException() throws InvocationTargetException {
            throw USER_INVOCATION_TARGET_EXCEPTION;
        }
    }

    @Before(ProceedInterceptor.class)
    public static class InterceptedFailureService extends PlainFailureService {
    }

    public static class ProceedInterceptor implements Interceptor {
        static int invocationCount;

        @Override
        public void intercept(Invocation inv) throws Exception {
            invocationCount++;
            inv.invoke();
        }
    }

    static class CheckedFailure extends Exception {
        private static final long serialVersionUID = 1L;
    }

    static class DirectThrowable extends Throwable {
        private static final long serialVersionUID = 1L;
    }

    @FunctionalInterface
    interface ThrowingCall {
        void run() throws Throwable;
    }
}

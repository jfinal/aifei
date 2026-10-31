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

package cn.aifei.aop;

import cn.aifei.argument.Argument;
import cn.aifei.router.Action;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class InvocationTest {

    @Test
    public void interceptorCanHandleActionRuntimeException() throws Exception {
        IllegalStateException failure = new IllegalStateException("business failure");
        Invocation invocation = action(new Target(failure), inv -> {
            try {
                inv.invoke();
            } catch (IllegalStateException actual) {
                assertSame(failure, actual);
                inv.setReturnValue("handled");
            }
        });

        invocation.invoke();

        assertEquals("handled", invocation.getReturnValue());
    }

    @Test
    public void checkedExceptionReachesInterceptorAndCallerUnchanged() throws Exception {
        IOException failure = new IOException("checked failure");
        Exception[] observed = new Exception[1];
        Invocation invocation = action(new Target(failure), inv -> {
            try {
                inv.invoke();
            } catch (IOException actual) {
                observed[0] = actual;
                throw actual;
            }
        });

        assertSameFailure(failure, invocation);
        assertSame(failure, observed[0]);
    }

    @Test
    public void actionInvocationTargetExceptionLosesOnlyReflectionWrapper() throws Exception {
        InvocationTargetException failure = new InvocationTargetException(new IllegalStateException());

        assertSameFailure(failure, action(new Target(failure)));
    }

    @Test
    public void actionErrorKeepsReflectionWrapper() throws Exception {
        assertReflectionWrapper(new AssertionError("action error"));
    }

    @Test
    public void actionDirectThrowableKeepsReflectionWrapper() throws Exception {
        assertReflectionWrapper(new Throwable("direct throwable"));
    }

    @Test
    public void interceptorInvocationTargetExceptionIsNotUnwrapped() throws Exception {
        InvocationTargetException failure = new InvocationTargetException(new IOException());
        Target target = new Target(null);
        Invocation invocation = action(target, inv -> { throw failure; });

        assertSameFailure(failure, invocation);
        assertEquals(0, target.calls);
    }

    @Test
    public void callbackInvocationTargetExceptionIsNotUnwrapped() throws Exception {
        InvocationTargetException failure = new InvocationTargetException(new IOException());
        Target target = new Target(null);
        Invocation invocation = new Invocation(target, Target.class.getMethod("run"),
                new Object[0], new Interceptor[0], args -> { throw failure; });

        assertSameFailure(failure, invocation);
    }

    @Test
    public void successfulActionRunsOnceAndKeepsReturnValue() throws Exception {
        Target target = new Target(null);
        Invocation invocation = action(target, inv -> {
            inv.invoke();
            inv.invoke();
        });

        invocation.invoke();
        invocation.invoke();

        assertSame(target.result, invocation.getReturnValue());
        assertEquals(1, target.calls);
    }

    private static Invocation action(Target target, Interceptor... interceptors) throws Exception {
        Method method = Target.class.getMethod("run");
        Action action = new Action("/test/run", "/test", interceptors, Target.class,
                method, new Argument<?, ?, ?>[0]);
        return new Invocation(action, target, null, null);
    }

    private static void assertSameFailure(Exception expected, Invocation invocation) {
        try {
            invocation.invoke();
            fail("Expected " + expected.getClass().getName());
        } catch (Exception actual) {
            assertSame(expected, actual);
        }
    }

    private static void assertReflectionWrapper(Throwable expected) throws Exception {
        try {
            action(new Target(expected)).invoke();
            fail("Expected InvocationTargetException");
        } catch (InvocationTargetException actual) {
            assertSame(expected, actual.getCause());
        }
    }

    public static class Target {
        final Throwable failure;
        final Object result = new Object();
        int calls;

        public Target(Throwable failure) {
            this.failure = failure;
        }

        public Object run() throws Throwable {
            calls++;
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }
}

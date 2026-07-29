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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class ProxyFactoryBehaviorTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> proxyFactories() {
        return Arrays.asList(new Object[][] {
                {"cglib", new CglibProxyFactory()},
                {"javassist", new JavassistProxyFactory()}
        });
    }

    private final ProxyFactory factory;

    public ProxyFactoryBehaviorTest(String name, ProxyFactory factory) {
        this.factory = factory;
    }

    @Test
    public void inheritedMethodCacheIsIsolatedByTargetClass() {
        FirstInterceptor.invocationCount = 0;
        SecondInterceptor.invocationCount = 0;

        assertEquals("first", factory.get(FirstService.class).work());
        assertEquals("second", factory.get(SecondService.class).work());

        assertEquals(1, FirstInterceptor.invocationCount);
        assertEquals(1, SecondInterceptor.invocationCount);
    }

    @Test
    public void emptyCacheEntryDoesNotHideInterceptorOnAnotherTargetClass() {
        GuardInterceptor.invocationCount = 0;

        assertEquals("plain", factory.get(PlainService.class).work());
        assertEquals("guarded", factory.get(GuardedService.class).work());

        assertEquals(1, GuardInterceptor.invocationCount);
    }

    @Test
    public void interceptedCacheEntryDoesNotLeakIntoPlainTargetClass() {
        ReverseGuardInterceptor.invocationCount = 0;

        assertEquals("guarded", factory.get(ReverseGuardedService.class).work());
        assertEquals("plain", factory.get(ReversePlainService.class).work());

        assertEquals(1, ReverseGuardInterceptor.invocationCount);
    }

    @Test
    public void classAndInheritedMethodInterceptorsKeepTheirOrder() {
        OrderRecorder.events.clear();

        assertEquals("done", factory.get(OrderService.class).work());

        assertEquals(Arrays.asList("class", "method", "target"), OrderRecorder.events);
    }

    @Test
    public void overloadedMethodsUseIndependentCacheEntries() {
        StringMethodInterceptor.invocationCount = 0;
        IntMethodInterceptor.invocationCount = 0;

        OverloadedService service = factory.get(OverloadedService.class);

        assertEquals("a!", service.convert("a"));
        assertEquals(42, service.convert(41));
        assertEquals(1, StringMethodInterceptor.invocationCount);
        assertEquals(1, IntMethodInterceptor.invocationCount);
    }

    @Test
    public void sharedCallbackDoesNotCaptureAProxyInstance() {
        StatefulInterceptor.invocationCount = 0;

        StatefulService first = factory.get(StatefulService.class);
        StatefulService second = factory.get(StatefulService.class);

        assertNotSame(first, second);
        assertEquals(1, first.increment());
        assertEquals(2, first.increment());
        assertEquals(1, second.increment());
        assertEquals(3, StatefulInterceptor.invocationCount);
    }

    @Test
    public void abstractMethodCanBeCompletedWithoutProceeding() {
        AbstractInterceptor.invocationCount = 0;

        AbstractService service = factory.get(AbstractService.class);

        assertEquals("handled", service.value());
        assertEquals(1, AbstractInterceptor.invocationCount);
    }

    @Test
    public void objectAndFinalMethodsAreNotIntercepted() {
        BusinessInterceptor.invocationCount = 0;

        ObjectMethodService service = factory.get(ObjectMethodService.class);

        assertTrue(service.equals(service));
        assertFalse(service.equals(new Object()));
        service.hashCode();
        service.toString();
        assertEquals("final", service.finalValue());
        assertEquals(0, BusinessInterceptor.invocationCount);

        assertEquals("business", service.business());
        assertEquals(1, BusinessInterceptor.invocationCount);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void requestingProxyClassDoesNotCreateNestedProxy() {
        NestedProxyInterceptor.invocationCount = 0;

        NestedProxyService first = factory.get(NestedProxyService.class);
        NestedProxyService second = (NestedProxyService) factory.get((Class) first.getClass());

        assertSame(NestedProxyService.class, second.getClass().getSuperclass());
        assertEquals("nested", second.work());
        assertEquals(1, NestedProxyInterceptor.invocationCount);
    }

    @Test
    public void plainMethodsPreserveArgumentsReturnValuesAndState() {
        PlainMethodService service = factory.get(PlainMethodService.class);

        assertEquals(7, service.add(3, 4));
        assertEquals("hello", service.echo("hello"));
        service.setValue(9);
        assertEquals(9, service.getValue());
    }

    public static class SharedBaseService {
        private final String value;

        protected SharedBaseService(String value) {
            this.value = value;
        }

        public String work() {
            return value;
        }
    }

    @Before(FirstInterceptor.class)
    public static class FirstService extends SharedBaseService {
        public FirstService() {
            super("first");
        }
    }

    @Before(SecondInterceptor.class)
    public static class SecondService extends SharedBaseService {
        public SecondService() {
            super("second");
        }
    }

    public static class FirstInterceptor implements Interceptor {
        static int invocationCount;

        @Override
        public void intercept(Invocation inv) throws Exception {
            invocationCount++;
            inv.invoke();
        }
    }

    public static class SecondInterceptor implements Interceptor {
        static int invocationCount;

        @Override
        public void intercept(Invocation inv) throws Exception {
            invocationCount++;
            inv.invoke();
        }
    }

    public static class EmptyFirstBaseService {
        private final String value;

        protected EmptyFirstBaseService(String value) {
            this.value = value;
        }

        public String work() {
            return value;
        }
    }

    public static class PlainService extends EmptyFirstBaseService {
        public PlainService() {
            super("plain");
        }
    }

    @Before(GuardInterceptor.class)
    public static class GuardedService extends EmptyFirstBaseService {
        public GuardedService() {
            super("guarded");
        }
    }

    public static class GuardInterceptor implements Interceptor {
        static int invocationCount;

        @Override
        public void intercept(Invocation inv) throws Exception {
            invocationCount++;
            inv.invoke();
        }
    }

    public static class InterceptedFirstBaseService {
        private final String value;

        protected InterceptedFirstBaseService(String value) {
            this.value = value;
        }

        public String work() {
            return value;
        }
    }

    @Before(ReverseGuardInterceptor.class)
    public static class ReverseGuardedService extends InterceptedFirstBaseService {
        public ReverseGuardedService() {
            super("guarded");
        }
    }

    public static class ReversePlainService extends InterceptedFirstBaseService {
        public ReversePlainService() {
            super("plain");
        }
    }

    public static class ReverseGuardInterceptor implements Interceptor {
        static int invocationCount;

        @Override
        public void intercept(Invocation inv) throws Exception {
            invocationCount++;
            inv.invoke();
        }
    }

    public static class OrderBaseService {
        @Before(MethodOrderInterceptor.class)
        public String work() {
            OrderRecorder.events.add("target");
            return "done";
        }
    }

    @Before(ClassOrderInterceptor.class)
    public static class OrderService extends OrderBaseService {
    }

    public static class ClassOrderInterceptor implements Interceptor {
        @Override
        public void intercept(Invocation inv) throws Exception {
            OrderRecorder.events.add("class");
            inv.invoke();
        }
    }

    public static class MethodOrderInterceptor implements Interceptor {
        @Override
        public void intercept(Invocation inv) throws Exception {
            OrderRecorder.events.add("method");
            inv.invoke();
        }
    }

    static class OrderRecorder {
        static final List<String> events = new ArrayList<>();
    }

    public static class OverloadedService {
        @Before(StringMethodInterceptor.class)
        public String convert(String value) {
            return value + "!";
        }

        @Before(IntMethodInterceptor.class)
        public int convert(int value) {
            return value + 1;
        }
    }

    public static class StringMethodInterceptor implements Interceptor {
        static int invocationCount;

        @Override
        public void intercept(Invocation inv) throws Exception {
            invocationCount++;
            inv.invoke();
        }
    }

    public static class IntMethodInterceptor implements Interceptor {
        static int invocationCount;

        @Override
        public void intercept(Invocation inv) throws Exception {
            invocationCount++;
            inv.invoke();
        }
    }

    @Before(StatefulInterceptor.class)
    public static class StatefulService {
        private int value;

        public int increment() {
            return ++value;
        }
    }

    public static class StatefulInterceptor implements Interceptor {
        static int invocationCount;

        @Override
        public void intercept(Invocation inv) throws Exception {
            invocationCount++;
            inv.invoke();
        }
    }

    @Before(AbstractInterceptor.class)
    public abstract static class AbstractService {
        public abstract String value();
    }

    public static class AbstractInterceptor implements Interceptor {
        static int invocationCount;

        @Override
        public void intercept(Invocation inv) {
            invocationCount++;
            inv.setReturnValue("handled");
        }
    }

    @Before(BusinessInterceptor.class)
    public static class ObjectMethodService {
        public String business() {
            return "business";
        }

        public final String finalValue() {
            return "final";
        }
    }

    public static class BusinessInterceptor implements Interceptor {
        static int invocationCount;

        @Override
        public void intercept(Invocation inv) throws Exception {
            invocationCount++;
            inv.invoke();
        }
    }

    @Before(NestedProxyInterceptor.class)
    public static class NestedProxyService {
        public String work() {
            return "nested";
        }
    }

    public static class NestedProxyInterceptor implements Interceptor {
        static int invocationCount;

        @Override
        public void intercept(Invocation inv) throws Exception {
            invocationCount++;
            inv.invoke();
        }
    }

    public static class PlainMethodService {
        private int value;

        public int add(int first, int second) {
            return first + second;
        }

        public String echo(String value) {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}

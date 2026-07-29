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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProxyConcurrencyTest {

    private static final int THREAD_COUNT = 24;

    @Test
    public void cglibInitializesCallbackAndMethodCacheOnceUnderConcurrency() throws Exception {
        CglibConcurrentInterceptor.constructorCount.set(0);
        CglibConcurrentInterceptor.invocationCount.set(0);
        CglibConcurrentService.targetCount.set(0);

        runConcurrently(new CglibProxyFactory(), CglibConcurrentService.class);

        assertEquals(1, CglibConcurrentInterceptor.constructorCount.get());
        assertEquals(THREAD_COUNT, CglibConcurrentInterceptor.invocationCount.get());
        assertEquals(THREAD_COUNT, CglibConcurrentService.targetCount.get());
    }

    @Test
    public void javassistInitializesCallbackAndMethodCacheOnceUnderConcurrency() throws Exception {
        JavassistConcurrentInterceptor.constructorCount.set(0);
        JavassistConcurrentInterceptor.invocationCount.set(0);
        JavassistConcurrentService.targetCount.set(0);

        runConcurrently(new JavassistProxyFactory(), JavassistConcurrentService.class);

        assertEquals(1, JavassistConcurrentInterceptor.constructorCount.get());
        assertEquals(THREAD_COUNT, JavassistConcurrentInterceptor.invocationCount.get());
        assertEquals(THREAD_COUNT, JavassistConcurrentService.targetCount.get());
    }

    private <T extends ConcurrentService> void runConcurrently(
            ProxyFactory factory,
            Class<T> serviceClass) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>(THREAD_COUNT);

        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return factory.get(serviceClass).work(41);
                }));
            }

            assertTrue("Workers did not become ready", ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            for (Future<Integer> future : futures) {
                assertEquals(Integer.valueOf(42), future.get(10, TimeUnit.SECONDS));
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue("Executor did not terminate", executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    interface ConcurrentService {
        int work(int value);
    }

    @Before(CglibConcurrentInterceptor.class)
    public static class CglibConcurrentService implements ConcurrentService {
        static final AtomicInteger targetCount = new AtomicInteger();

        @Override
        public int work(int value) {
            targetCount.incrementAndGet();
            return value + 1;
        }
    }

    public static class CglibConcurrentInterceptor implements Interceptor {
        static final AtomicInteger constructorCount = new AtomicInteger();
        static final AtomicInteger invocationCount = new AtomicInteger();

        public CglibConcurrentInterceptor() {
            constructorCount.incrementAndGet();
        }

        @Override
        public void intercept(Invocation inv) throws Exception {
            invocationCount.incrementAndGet();
            inv.invoke();
        }
    }

    @Before(JavassistConcurrentInterceptor.class)
    public static class JavassistConcurrentService implements ConcurrentService {
        static final AtomicInteger targetCount = new AtomicInteger();

        @Override
        public int work(int value) {
            targetCount.incrementAndGet();
            return value + 1;
        }
    }

    public static class JavassistConcurrentInterceptor implements Interceptor {
        static final AtomicInteger constructorCount = new AtomicInteger();
        static final AtomicInteger invocationCount = new AtomicInteger();

        public JavassistConcurrentInterceptor() {
            constructorCount.incrementAndGet();
        }

        @Override
        public void intercept(Invocation inv) throws Exception {
            invocationCount.incrementAndGet();
            inv.invoke();
        }
    }
}

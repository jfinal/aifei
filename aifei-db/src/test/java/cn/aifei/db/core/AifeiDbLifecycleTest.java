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

package cn.aifei.db.core;

import cn.aifei.db.ext.NullDataSource;
import cn.aifei.log.Log;
import cn.aifei.log.LogFactory;
import cn.aifei.log.LogKit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class AifeiDbLifecycleTest {

    private static final AtomicInteger ID_SEQUENCE = new AtomicInteger();
    private static LogFactory originalLogFactory;

    private final List<String> configIds = new ArrayList<>();

    @BeforeClass
    public static void installNoOpLogFactory() {
        originalLogFactory = LogKit.get().getLogFactory();
        Log noOpLog = (Log) Proxy.newProxyInstance(
                AifeiDbLifecycleTest.class.getClassLoader(),
                new Class<?>[]{Log.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
        LogFactory noOpLogFactory = (LogFactory) Proxy.newProxyInstance(
                AifeiDbLifecycleTest.class.getClassLoader(),
                new Class<?>[]{LogFactory.class},
                (proxy, method, args) -> noOpLog);
        LogKit.get().setLogFactory(noOpLogFactory);
    }

    @AfterClass
    public static void restoreLogFactory() {
        LogKit.get().setLogFactory(originalLogFactory);
    }

    @After
    public void cleanUpConfigs() {
        for (String configId : configIds) {
            DbKit.removeConfig(configId);
        }
        DbKit.config = DbKit.fakeConfig;
    }

    @Test
    public void startAndStopAreIdempotent() {
        String configId = newConfigId();
        AifeiDb db = new AifeiDb(configId, NullDataSource.instance);

        db.start();
        DbConfig registered = DbKit.getConfig(configId);
        db.start();

        assertSame(db.getConfig(), registered);
        assertSame(registered, DbKit.getConfig(configId));

        db.stop();
        db.stop();
        assertNull(DbKit.getConfig(configId));
    }

    @Test
    public void stoppingAnInstanceThatNeverStartedCannotRemoveAnotherInstancesConfig() {
        String configId = newConfigId();
        AifeiDb started = new AifeiDb(configId, NullDataSource.instance);
        AifeiDb neverStarted = new AifeiDb(configId, NullDataSource.instance);
        started.start();

        neverStarted.stop();

        assertSame(started.getConfig(), DbKit.getConfig(configId));
        started.stop();
        assertNull(DbKit.getConfig(configId));
    }

    @Test(timeout = 5000)
    public void concurrentStartsAndStopsExecuteEachLifecycleTransitionOnce() throws Exception {
        String configId = newConfigId();
        AifeiDb db = new AifeiDb(configId, NullDataSource.instance);

        runConcurrently(12, db::start);
        assertSame(db.getConfig(), DbKit.getConfig(configId));

        runConcurrently(12, db::stop);
        assertNull(DbKit.getConfig(configId));
    }

    private String newConfigId() {
        String configId = "lifecycle-test-" + ID_SEQUENCE.incrementAndGet();
        configIds.add(configId);
        return configId;
    }

    private static void runConcurrently(int taskCount, Runnable action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(taskCount);
        CountDownLatch ready = new CountDownLatch(taskCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < taskCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    action.run();
                    return null;
                }));
            }
            ready.await();
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }
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
}

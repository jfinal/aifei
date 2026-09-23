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

import cn.aifei.proxy.fixture.AccessFixture;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;

@RunWith(Parameterized.class)
public class InstanceFactoryTest {

    @Parameterized.Parameters(name = "jit={0}")
    public static Collection<Object[]> modes() {
        return Arrays.asList(new Object[][] {{true}, {false}});
    }

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final boolean jit;
    private final InstanceFactory factory = new InstanceFactory();
    private boolean previousJit;

    public InstanceFactoryTest(boolean jit) {
        this.jit = jit;
    }

    @Before
    public void configureMode() {
        previousJit = InstanceFactory.jit;
        InstanceFactory.setJit(jit);
    }

    @After
    public void restoreMode() {
        InstanceFactory.setJit(previousJit);
    }

    @Test
    public void eachCallCreatesANewInstance() {
        Service.constructorCount.set(0);
        Service first = factory.get(Service.class);
        Service second = factory.get(Service.class);

        assertNotSame(first, second);
        assertEquals(2, Service.constructorCount.get());
    }

    @Test
    public void acceptsImplicitPublicConstructorsAndJdkClasses() {
        assertSame(DefaultConstructorService.class, factory.get(DefaultConstructorService.class).getClass());
        assertTrue(factory.get(ArrayList.class).isEmpty());
    }

    @Test
    public void acceptsAccessiblePackagePrivateClassesWithPublicConstructors() {
        assertSame(PackageService.class, factory.get(PackageService.class).getClass());
    }

    @Test
    public void rejectsNonPublicAndMissingNoArgConstructors() {
        for (Class<?> type : Arrays.asList(PrivateConstructorService.class,
                ProtectedConstructorService.class, PackageConstructorService.class, ArgumentService.class)) {
            RuntimeException failure = assertThrows(RuntimeException.class, () -> factory.get(type));
            assertEquals(NoSuchMethodException.class, failure.getCause().getClass());
        }
    }

    @Test
    public void doesNotWidenAccessToAnInaccessibleClass() {
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> factory.get(AccessFixture.inaccessibleClass()));
        assertEquals(IllegalAccessException.class, failure.getCause().getClass());
    }

    @Test
    public void abstractClassesFailThroughReflection() {
        RuntimeException failure = assertThrows(RuntimeException.class, () -> factory.get(AbstractService.class));
        assertEquals(InstantiationException.class, failure.getCause().getClass());
    }

    @Test
    public void checkedConstructorFailuresKeepTheirPathSpecificWrappingWithoutRetry() {
        assertConstructorFailure(new IOException("constructor"), jit);
    }

    @Test
    public void runtimeConstructorFailuresKeepTheirPathSpecificWrappingWithoutRetry() {
        assertConstructorFailure(new IllegalStateException("constructor"), jit);
    }

    @Test
    public void constructorErrorsKeepTheirPathSpecificWrappingWithoutRetry() {
        assertConstructorFailure(new AssertionError("constructor"), jit);
    }

    @Test
    public void switchingModesUsesTheRequestedStrategyEvenAfterCaching() {
        assertConstructorFailure(new IOException("initial mode"), jit);
        InstanceFactory.setJit(!jit);
        assertConstructorFailure(new IOException("other mode"), !jit);
        InstanceFactory.setJit(jit);
        assertConstructorFailure(new IOException("restored mode"), jit);
    }

    @Test
    public void classInitializationFailureIsNotRetried() {
        Class<?> type = jit ? LambdaInitializationFailure.class : ReflectionInitializationFailure.class;
        InitializationTracker.calls.set(0);
        ExceptionInInitializerError failure = assertThrows(ExceptionInInitializerError.class, () -> factory.get(type));
        assertEquals(IllegalStateException.class, failure.getCause().getClass());
        assertEquals(1, InitializationTracker.calls.get());
    }

    @Test
    public void sameNameFromAnotherLoaderCreatesTheRequestedClass() throws Exception {
        Class<?> type = isolatedCopy(LoaderService.class);
        assertNotSame(LoaderService.class, type);
        Object first = factory.get(type);
        Object second = factory.get(type);
        assertSame(type, first.getClass());
        assertSame(type, second.getClass());
        assertNotSame(first, second);

        Supplier<?> supplier = (jit ? InstanceFactory.CACHE : InstanceFactory.REFLECTION_CACHE).get(type);
        for (Field field : supplier.getClass().getDeclaredFields()) {
            assertFalse("Supplier must not retain the factory", InstanceFactory.class.isAssignableFrom(field.getType()));
        }
    }

    @Test
    public void reflectionFallbackDoesNotRetryConstructorFailures() throws Exception {
        Class<?> type = isolatedCopy(LoaderFailureService.class);
        RuntimeException failure = assertThrows(RuntimeException.class, () -> factory.get(type));
        assertEquals(InvocationTargetException.class, failure.getCause().getClass());
        assertEquals(IllegalStateException.class, failure.getCause().getCause().getClass());
        assertEquals(1, type.getField("calls").getInt(null));
    }

    @Test
    public void classVisibleOnlyToAChildLoaderUsesReflection() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assumeNotNull(compiler);
        Path directory = temporaryFolder.newFolder("child").toPath();
        Path source = directory.resolve("ChildService.java");
        Files.write(source, ("package child; public class ChildService { public ChildService() {} }")
                .getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        assertEquals(diagnostics.toString("UTF-8"), 0,
                compiler.run(null, diagnostics, diagnostics, "-d", directory.toString(), source.toString()));

        try (URLClassLoader loader = new URLClassLoader(new URL[] {directory.toUri().toURL()},
                InstanceFactory.class.getClassLoader())) {
            Class<?> type = loader.loadClass("child.ChildService");
            assertThrows(ClassNotFoundException.class,
                    () -> Class.forName(type.getName(), false, InstanceFactory.class.getClassLoader()));
            assertSame(type, factory.get(type).getClass());
        }
    }

    @Test
    public void noProxyFactoryStillCreatesFreshServiceInstances() {
        NoProxyFactory noProxy = new NoProxyFactory();
        assertNotSame(noProxy.get(Service.class), noProxy.get(Service.class));
    }

    @Test
    public void concurrentFirstUseCreatesOneObjectPerCall() throws Exception {
        int count = 12;
        ConcurrentService.constructorCount.set(0);
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ConcurrentService>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < count; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    return factory.get(ConcurrentService.class);
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            Set<ConcurrentService> instances = Collections.newSetFromMap(new IdentityHashMap<ConcurrentService, Boolean>());
            for (Future<ConcurrentService> future : futures) {
                assertTrue(instances.add(future.get(10, TimeUnit.SECONDS)));
            }
            assertEquals(count, ConcurrentService.constructorCount.get());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private void assertConstructorFailure(Throwable expected, boolean lambda) {
        FailureService.failure = expected;
        FailureService.calls = 0;
        Throwable actual = assertThrows(Throwable.class, () -> factory.get(FailureService.class));
        if (lambda) {
            assertSame(expected, actual);
        } else {
            assertEquals(RuntimeException.class, actual.getClass());
            assertEquals(InvocationTargetException.class, actual.getCause().getClass());
            assertSame(expected, actual.getCause().getCause());
        }
        assertEquals("Constructor must run exactly once", 1, FailureService.calls);
    }

    private Class<?> isolatedCopy(Class<?> type) throws IOException, ClassNotFoundException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream input = type.getResourceAsStream(resource)) {
            assertNotNull(input);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
        }
        byte[] bytes = output.toByteArray();
        return new ClassLoader(type.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.equals(type.getName())) {
                    return super.loadClass(name, resolve);
                }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> result = findLoadedClass(name);
                    if (result == null) {
                        result = defineClass(name, bytes, 0, bytes.length);
                    }
                    if (resolve) {
                        resolveClass(result);
                    }
                    return result;
                }
            }
        }.loadClass(type.getName());
    }

    public static class Service {
        static final AtomicInteger constructorCount = new AtomicInteger();
        public Service() { constructorCount.incrementAndGet(); }
    }

    public static class DefaultConstructorService {}
    static class PackageService { public PackageService() {} }
    public static class PrivateConstructorService { private PrivateConstructorService() {} }
    public static class ProtectedConstructorService { protected ProtectedConstructorService() {} }
    public static class PackageConstructorService { PackageConstructorService() {} }
    public static class ArgumentService { public ArgumentService(String value) {} }
    public abstract static class AbstractService { public AbstractService() {} }

    public static class FailureService {
        static Throwable failure;
        static int calls;
        public FailureService() throws Throwable { calls++; throw failure; }
    }

    public static class LoaderService { public LoaderService() {} }

    public static class LoaderFailureService {
        public static int calls;
        public LoaderFailureService() { calls++; throw new IllegalStateException("constructor"); }
    }

    public static class ConcurrentService {
        static final AtomicInteger constructorCount = new AtomicInteger();
        public ConcurrentService() { constructorCount.incrementAndGet(); }
    }

    public static class InitializationTracker {
        static final AtomicInteger calls = new AtomicInteger();
        static void fail() { calls.incrementAndGet(); throw new IllegalStateException("initializer"); }
    }

    public static class LambdaInitializationFailure {
        static { InitializationTracker.fail(); }
        public LambdaInitializationFailure() {}
    }

    public static class ReflectionInitializationFailure {
        static { InitializationTracker.fail(); }
        public ReflectionInitializationFailure() {}
    }
}

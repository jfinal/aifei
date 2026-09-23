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

package cn.aifei.enjoy.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class InstanceUtilTest {

    @Parameterized.Parameters(name = "jit={0}")
    public static Collection<Object[]> modes() {
        return Arrays.asList(new Object[][] {{true}, {false}});
    }

    private final boolean jit;
    private boolean previousJit;

    public InstanceUtilTest(boolean jit) {
        this.jit = jit;
    }

    @Before
    public void configureMode() {
        previousJit = InstanceUtil.jit;
        InstanceUtil.setJit(jit);
    }

    @After
    public void restoreMode() {
        InstanceUtil.setJit(previousJit);
    }

    @Test
    public void eachCallCreatesANewInstanceUsingThePublicDefaultConstructor() {
        assertNotSame(InstanceUtil.get(Service.class), InstanceUtil.get(Service.class));
    }

    @Test
    public void rejectsNonPublicAndMissingNoArgConstructors() {
        for (Class<?> type : Arrays.asList(PrivateConstructor.class, ArgumentConstructor.class)) {
            RuntimeException failure = assertThrows(RuntimeException.class, () -> InstanceUtil.get(type));
            assertEquals(NoSuchMethodException.class, failure.getCause().getClass());
        }
    }

    @Test
    public void abstractClassesFailThroughReflection() {
        RuntimeException failure = assertThrows(RuntimeException.class, () -> InstanceUtil.get(AbstractService.class));
        assertEquals(InstantiationException.class, failure.getCause().getClass());
    }

    @Test
    public void constructorFailuresAreNotRetriedAndKeepTheirPathSpecificWrapping() {
        for (Throwable expected : Arrays.asList(new IOException("checked"),
                new IllegalStateException("runtime"), new AssertionError("error"))) {
            assertFailure(expected, jit);
        }
    }

    @Test
    public void switchingModesHonorsTheRequestedStrategyAfterCaching() {
        assertFailure(new IOException("initial"), jit);
        InstanceUtil.setJit(!jit);
        assertFailure(new IOException("other"), !jit);
        InstanceUtil.setJit(jit);
        assertFailure(new IOException("restored"), jit);
    }

    @Test
    public void sameNameFromAnotherLoaderCreatesTheRequestedClass() throws IOException {
        Class<?> type = isolatedCopy(Service.class);
        assertNotSame(Service.class, type);
        Object first = InstanceUtil.get(type);
        Object second = InstanceUtil.get(type);
        assertSame(type, first.getClass());
        assertSame(type, second.getClass());
        assertNotSame(first, second);
    }

    @Test
    public void reflectionFallbackDoesNotRetryConstructorFailures() throws Exception {
        Class<?> type = isolatedCopy(LoaderFailure.class);
        RuntimeException failure = assertThrows(RuntimeException.class, () -> InstanceUtil.get(type));
        assertEquals(InvocationTargetException.class, failure.getCause().getClass());
        assertEquals(IllegalStateException.class, failure.getCause().getCause().getClass());
        assertEquals(1, type.getField("calls").getInt(null));
    }

    private void assertFailure(Throwable expected, boolean lambda) {
        FailureService.failure = expected;
        FailureService.calls = 0;
        Throwable actual = assertThrows(Throwable.class, () -> InstanceUtil.get(FailureService.class));
        if (lambda) {
            assertSame(expected, actual);
        } else {
            assertEquals(RuntimeException.class, actual.getClass());
            assertEquals(InvocationTargetException.class, actual.getCause().getClass());
            assertSame(expected, actual.getCause().getCause());
        }
        assertEquals(1, FailureService.calls);
    }

    private Class<?> isolatedCopy(Class<?> type) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream input = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
            assertNotNull(input);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
        }
        byte[] bytes = output.toByteArray();
        return new ClassLoader(type.getClassLoader()) {
            Class<?> defineCopy() {
                return defineClass(type.getName(), bytes, 0, bytes.length);
            }
        }.defineCopy();
    }

    public static class Service {}
    public static class PrivateConstructor { private PrivateConstructor() {} }
    public static class ArgumentConstructor { public ArgumentConstructor(String value) {} }
    public abstract static class AbstractService { public AbstractService() {} }

    public static class FailureService {
        static Throwable failure;
        static int calls;
        public FailureService() throws Throwable { calls++; throw failure; }
    }

    public static class LoaderFailure {
        public static int calls;
        public LoaderFailure() { calls++; throw new IllegalStateException("constructor"); }
    }
}

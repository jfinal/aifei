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

package cn.aifei.proxy.javassist;

import javassist.util.proxy.ProxyObject;
import org.junit.Test;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class JavassistCallbackTest {

    @Test
    public void callbackIsSharedPerTargetClassAndIsolatedAcrossClasses() {
        JavassistCallback first = JavassistCallback.get(FirstTarget.class);
        JavassistCallback same = JavassistCallback.get(FirstTarget.class);
        JavassistCallback second = JavassistCallback.get(SecondTarget.class);

        assertSame(first, same);
        assertNotSame(first, second);
        assertSame(FirstTarget.class, first.targetClass);
        assertSame(SecondTarget.class, second.targetClass);
        assertNotSame(first.methodCache, second.methodCache);
    }

    @Test
    public void proxyFactoryInstallsTargetScopedCallback() {
        FirstTarget proxy = new JavassistProxyFactory().get(FirstTarget.class);

        assertSame(JavassistCallback.get(FirstTarget.class), ((ProxyObject) proxy).getHandler());
    }

    public static class FirstTarget {
    }

    public static class SecondTarget {
    }
}

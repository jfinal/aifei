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

package cn.aifei.proxy.cglib;

import net.sf.cglib.proxy.Factory;
import org.junit.Test;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class CglibCallbackTest {

    @Test
    public void callbackIsSharedPerTargetClassAndIsolatedAcrossClasses() {
        CglibCallback first = CglibCallback.get(FirstTarget.class);
        CglibCallback same = CglibCallback.get(FirstTarget.class);
        CglibCallback second = CglibCallback.get(SecondTarget.class);

        assertSame(first, same);
        assertNotSame(first, second);
        assertSame(FirstTarget.class, first.targetClass);
        assertSame(SecondTarget.class, second.targetClass);
        assertNotSame(first.interceptorCache, second.interceptorCache);
    }

    @Test
    public void proxyFactoryInstallsTargetScopedCallback() {
        FirstTarget proxy = new CglibProxyFactory().get(FirstTarget.class);

        assertSame(CglibCallback.get(FirstTarget.class), ((Factory) proxy).getCallback(0));
    }

    public static class FirstTarget {
    }

    public static class SecondTarget {
    }
}

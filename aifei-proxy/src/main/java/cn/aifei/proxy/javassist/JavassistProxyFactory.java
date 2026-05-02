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

import cn.aifei.proxy.InstanceFactory;
import cn.aifei.proxy.ProxyFactory;
import cn.aifei.util.ComputeCache;
import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.ProxyObject;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * JavassistProxyFactory 基于 javassist 实现 aop 代理
 *
 * <pre>
 * 配置方法：
 * public void config(Settings settings) {
 *     settings.setProxyFactory(new JavassistProxyFactory());
 * }
 * </pre>
 */
public class JavassistProxyFactory implements ProxyFactory {

    InstanceFactory instanceFactory = new InstanceFactory();
    JavassistCallback callback = new JavassistCallback();
    JavassistMethodFilter methodFilter = new JavassistMethodFilter();
    ComputeCache<Class<?>, Class<?>> cache = new ComputeCache<>(512);

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> target) {
        // 被 javassist 代理过的类名包含 "_$$_"。不存在代理过两层的情况，仅需调用一次 getSuperclass() 即可
        if (target.getName().contains("_$$_")) {
            target = (Class<T>) target.getSuperclass();
        }

        try {
            Class<T> clazz = (Class<T>) cache.get(target);
            if (clazz == null) {
                clazz = getProxyClass(target);
            }

            T ret = instanceFactory.get(clazz);
            ((ProxyObject) ret).setHandler(callback);
            return ret;

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> Class<T> getProxyClass(Class<T> target) throws ReflectiveOperationException {
        return (Class<T>) cache.computeIfAbsent(target, key -> {
            javassist.util.proxy.ProxyFactory factory = new javassist.util.proxy.ProxyFactory();
            factory.setSuperclass(key);
            factory.setFilter(methodFilter);
            return factory.createClass();
        });
    }

    /**
     * 过滤不需要代理的方法，参考资料：
     * https://github.com/jboss-javassist/javassist/blob/master/src/test/testproxy/ProxyTester.java
     */
    private static class JavassistMethodFilter implements MethodFilter {

        private static final Set<String> excludedMethodName = buildExcludedMethodName();

        @Override
        public boolean isHandled(Method method) {
            return !excludedMethodName.contains(method.getName());
        }

        private static Set<String> buildExcludedMethodName() {
            Set<String> excludedMethodName = new HashSet<>(64, 2F / 3F);
            Method[] methods = Object.class.getDeclaredMethods();
            for (Method m : methods) {
                excludedMethodName.add(m.getName());
            }
            // getClass() registerNatives() can not be enhanced
            // excludedMethodName.remove("getClass");
            // excludedMethodName.remove("registerNatives");
            return excludedMethodName;
        }
    }
}






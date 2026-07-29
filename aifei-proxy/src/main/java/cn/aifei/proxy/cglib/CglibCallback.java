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

import cn.aifei.aop.Interceptor;
import cn.aifei.aop.InterceptorKit;
import cn.aifei.aop.Invocation;
import cn.aifei.util.ComputeCache;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CglibCallback.
 */
class CglibCallback implements MethodInterceptor {

    static final Set<String> excludedMethodName = buildExcludedMethodName();
    static final InterceptorKit interKit = InterceptorKit.get();
    static final ComputeCache<Class<?>, CglibCallback> callbackCache = new ComputeCache<>(512);

    final Class<?> targetClass;
    final ConcurrentHashMap<Method, Interceptor[]> methodCache = new ConcurrentHashMap<>();

    private CglibCallback(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    static CglibCallback get(Class<?> targetClass) {
        return callbackCache.computeIfAbsent(targetClass, CglibCallback::new);
    }

    public Object intercept(Object target, Method method, Object[] args, MethodProxy methodProxy) throws Exception {
        if (excludedMethodName.contains(method.getName())) {
            return invokeSuper(methodProxy, target, args);
        }

        Interceptor[] inters = methodCache.get(method);
        if (inters == null) {
            inters = cacheInterceptors(method);
        }

        if (inters.length == 0) {
            return invokeSuper(methodProxy, target, args);
        }

        Invocation invocation = new Invocation(target, method, args, inters, x -> {
            return invokeSuper(methodProxy, target, x);
        });
        invocation.invoke();

        return invocation.getReturnValue();
    }

    private Interceptor[] cacheInterceptors(Method method) {
        synchronized (methodCache) {
            Interceptor[] inters = methodCache.get(method);
            if (inters == null) {
                inters = interKit.buildServiceMethodInterceptor(targetClass, method);
                methodCache.put(method, inters);
            }
            return inters;
        }
    }

    private Object invokeSuper(MethodProxy methodProxy, Object target, Object[] args) throws Exception {
        try {
            return methodProxy.invokeSuper(target, args);
        } catch (Exception e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
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


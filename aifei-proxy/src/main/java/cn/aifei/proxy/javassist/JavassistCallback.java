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

import cn.aifei.aop.Interceptor;
import cn.aifei.aop.InterceptorKit;
import cn.aifei.aop.Invocation;
import cn.aifei.util.ComputeCache;
import javassist.util.proxy.MethodHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JavassistCallback.
 */
class JavassistCallback implements MethodHandler {

    static final InterceptorKit interKit = InterceptorKit.get();
    static final ComputeCache<Class<?>, JavassistCallback> callbackCache = new ComputeCache<>(512);

    final Class<?> targetClass;
    final ConcurrentHashMap<Method, Interceptor[]> methodCache = new ConcurrentHashMap<>();

    private JavassistCallback(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    static JavassistCallback get(Class<?> targetClass) {
        return callbackCache.computeIfAbsent(targetClass, JavassistCallback::new);
    }

    @Override
    public Object invoke(Object target, Method method, Method methodProxy, Object[] args) throws Exception {
        Interceptor[] inters = methodCache.get(method);
        if (inters == null) {
            inters = cacheInterceptors(method);
        }

        if (inters.length == 0) {
            return invokeMethod(methodProxy, target, args);
        }

        Invocation invocation = new Invocation(target, method, args, inters, x -> {
            return invokeMethod(methodProxy, target, x);
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

    /**
     * Method.invoke() 会用 InvocationTargetException 包装目标方法异常，
     * 此处只拆除当前反射调用产生的一层包装。
     */
    private Object invokeMethod(Method method, Object target, Object[] args) throws Exception {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof Exception) {
                throw (Exception) targetException;
            }
            if (targetException instanceof Error) {
                throw (Error) targetException;
            }
            throw new RuntimeException(targetException);
        }
    }
}

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

/**
 * CglibCallback.
 */
class CglibCallback implements MethodInterceptor {

    static final Set<String> excludedMethodName = buildExcludedMethodName();

    static final InterceptorKit interKit = InterceptorKit.get();
    static final ComputeCache<Method, Interceptor[]> cache = new ComputeCache<>(512);

    public Object intercept(Object target, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        if (excludedMethodName.contains(method.getName())) {
            return methodProxy.invokeSuper(target, args);
        }

        Interceptor[] inters = cache.computeIfAbsent(method, m -> {
            // Class<?> targetClass = target.getClass();
            // if (targetClass.getName().contains("$$Enhance")) {
            //     targetClass = targetClass.getSuperclass();
            // }

            Class<?> targetClass = target.getClass().getSuperclass();
            return interKit.buildServiceMethodInterceptor(targetClass, m);
        });

        if (inters.length == 0) {
            return methodProxy.invokeSuper(target, args);
        }

        Invocation invocation = new Invocation(target, method, args, inters, x -> {
            return methodProxy.invokeSuper(target, x);
        });
        invocation.invoke();

        return invocation.getReturnValue();
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




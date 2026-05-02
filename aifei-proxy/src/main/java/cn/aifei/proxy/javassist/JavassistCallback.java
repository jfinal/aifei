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
import javassist.util.proxy.MethodHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JavassistCallback.
 */
class JavassistCallback implements MethodHandler {

    static final InterceptorKit interKit = InterceptorKit.get();
    static final ConcurrentHashMap<Method, Interceptor[]> cache = new ConcurrentHashMap<>();

    @Override
    public Object invoke(Object target, Method method, Method methodProxy, Object[] args) throws Throwable {
        Interceptor[] inters = cache.get(method);
        if (inters == null) {
            Class<?> targetClass = target.getClass().getSuperclass();
            inters = interKit.buildServiceMethodInterceptor(targetClass, method);
            cache.put(method, inters);
        }

        if (inters.length == 0) {
            return methodProxy.invoke(target, args);
        }

        Invocation invocation = new Invocation(target, method, args, inters, x -> {
            return methodProxy.invoke(target, x);
        });
        invocation.invoke();

        return invocation.getReturnValue();
    }
}




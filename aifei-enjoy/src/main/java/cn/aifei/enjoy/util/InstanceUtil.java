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

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * InstanceUtil 用于通过无参构造方法创建对象。
 *
 * <pre>
 * 设计：
 *  1: LambdaMetaFactory + Supplier 促使 JIT 将 supplier.get() 内联为
 *     new YourClass()，开销几乎为 0。
 *
 *  2: CACHE 缓存构造器对象，避免每次重新获取。
 *
 *  3: MethodHandles.privateLookupIn 方法对 JPMS 适应性更好。不要使用
 *     getDeclaredConstructor()、getConstructor()，若要使用，前者比后者好。
 * </pre>
 */
public class InstanceUtil {

    static final MethodType METHOD_TYPE_VOID = MethodType.methodType(void.class);
    static final MethodType METHOD_TYPE_SUPPLIER = MethodType.methodType(Supplier.class);
    static final MethodType METHOD_TYPE_OBJECT = MethodType.methodType(Object.class);

    static final Method PRIVATE_LOOKUP_IN = findPrivateLookupIn();
    static final ComputeCache<Class<?>, Supplier<?>> CACHE = new ComputeCache<>(512);
    static final ConcurrentHashMap<Class<?>, Supplier<?>> FACTORY_CACHE = new ConcurrentHashMap<>(64);

    static boolean jit = true;

    // JDK 9 及更高版本通过 MethodHandles.privateLookupIn(...) 更适应 JPMS
    private static Method findPrivateLookupIn() {
        try {
            return MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
        } catch (Exception e) {
            return null; // JDK 8 将 PRIVATE_LOOKUP_IN 赋为 null
        }
    }

    /**
     * 全局配置是否使用 jit，默认值为 true
     */
    public static void setJit(boolean jit) {
        InstanceUtil.jit = jit;
    }

    /**
     * 为指定类型注册自定义工厂
     */
    public static <T> void setFactory(Class<T> type, Supplier<T> factory) {
        FACTORY_CACHE.put(type, factory);
    }

    /**
     * 移除自定义工厂
     */
    public static <T> void removeFactory(Class<T> type) {
        FACTORY_CACHE.remove(type);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> type) {
        Supplier<?> factory = FACTORY_CACHE.get(type);
        if (factory != null) {
            return (T) factory.get();
        }

        if (jit) {
            return ((Supplier<T>) CACHE.computeIfAbsent(type, InstanceUtil::createSupplier)).get();
        } else {
            return newInstance(type);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Supplier<T> createSupplier(Class<T> type) {
        try {
            // 获取 Lookup。JDK 8 使用 lookup()，JDK 9 以上使用 privateLookupIn()
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            if (PRIVATE_LOOKUP_IN != null) {
                lookup = (MethodHandles.Lookup) PRIVATE_LOOKUP_IN.invoke(null, type, lookup);
            }

            // 获取无参构造器的 MethodHandle，无权访问抛出异常
            MethodHandle handle = lookup.findConstructor(type, METHOD_TYPE_VOID);
            MethodType methodType = MethodType.methodType(type);

            // LambdaMetaFactory 包成 Supplier.get() 促使 JIT 内联优化
            CallSite callSite = LambdaMetafactory.metafactory(
                    lookup,
                    "get",
                    METHOD_TYPE_SUPPLIER,
                    METHOD_TYPE_OBJECT,
                    handle,
                    methodType
            );

            return (Supplier<T>) callSite.getTarget().invokeExact();

        } catch (Throwable t) {
            throw new RuntimeException("Failed to prepare constructor for " + type, t);
        }
    }

    private static <T> T newInstance(Class<T> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}





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

import cn.aifei.util.ComputeCache;
import java.lang.invoke.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

/**
 * InstanceFactory 用于通过可访问的 public 无参构造方法创建对象。
 *
 * <pre>
 * 设计：
 *  1: 优先使用 LambdaMetafactory + Supplier，为 JIT 内联提供优化机会。
 *
 *  2: 按目标类型与 jit 模式分别缓存创建策略，每次调用仍创建新对象。
 *
 *  3: Lambda 不适用或准备阶段抛出非 Error 异常时回退到普通反射，不扩大访问权限。
 * </pre>
 */
public class InstanceFactory {

    static final MethodType METHOD_TYPE_SUPPLIER = MethodType.methodType(Supplier.class);
    static final MethodType METHOD_TYPE_OBJECT = MethodType.methodType(Object.class);

    static final ComputeCache<Class<?>, Supplier<?>> CACHE = new ComputeCache<>(512);
    static final ComputeCache<Class<?>, Supplier<?>> REFLECTION_CACHE = new ComputeCache<>(512);

    static volatile boolean jit = true;

    /**
     * 全局配置对象创建策略：true 优先使用 Lambda，false 仅使用普通反射。
     * 默认值为 true，不影响 JVM 自身的 JIT 编译。
     */
    public static void setJit(boolean jit) {
        InstanceFactory.jit = jit;
    }

    /**
     * 通过可访问的 public 无参构造器创建新对象。
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        if (jit) {
            return ((Supplier<T>) CACHE.computeIfAbsent(type, this::createSupplier)).get();
        } else {
            return ((Supplier<T>) REFLECTION_CACHE.computeIfAbsent(type, this::createReflectionSupplier)).get();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Supplier<T> createSupplier(Class<T> type) {
        Constructor<T> constructor = getConstructor(type);

        try {
            if (!Modifier.isAbstract(type.getModifiers()) && isVisible(type)) {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodHandle handle = lookup.unreflectConstructor(constructor);
                CallSite callSite = LambdaMetafactory.metafactory(
                        lookup,
                        "get",
                        METHOD_TYPE_SUPPLIER,
                        METHOD_TYPE_OBJECT,
                        handle,
                        MethodType.methodType(type)
                );
                return (Supplier<T>) callSite.getTarget().invokeExact();
            }

        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            // Lambda 仅作性能优化，反射是基础创建方式，负责兜底。
        }

        // 只在准备阶段回退，构造器执行失败时不重试。
        return () -> newInstance(constructor);
    }

    /**
     * 获取 public 无参构造器，不改变访问权限，是否可调用由后续访问检查决定。
     */
    private <T> Constructor<T> getConstructor(Class<T> type) {
        try {
            return type.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isVisible(Class<?> type) throws ClassNotFoundException {
        // Lambda 使用工厂的类加载器解析目标类。
        // 同名类可能由不同 ClassLoader 加载，必须比较 Class 身份。
        return Class.forName(type.getName(), false, InstanceFactory.class.getClassLoader()) == type;
    }

    private <T> Supplier<T> createReflectionSupplier(Class<T> type) {
        Constructor<T> constructor = getConstructor(type);
        return () -> newInstance(constructor);
    }

    // 使用静态方法，避免反射 Supplier 捕获工厂实例。
    private static <T> T newInstance(Constructor<T> constructor) {
        try {
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}



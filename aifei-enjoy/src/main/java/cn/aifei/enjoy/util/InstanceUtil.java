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
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

/**
 * 通过对工厂可访问的 public 无参构造器创建对象。
 *
 * <pre>
 * 设计：
 *  1: 默认优先使用 LambdaMetafactory 创建 Supplier，为 JVM 的 JIT 内联提供优化机会。
 *
 *  2: 按目标 Class 缓存 Supplier，由所有工厂实例共享；jit=true 与 jit=false 使用独立缓存。
 *     仅缓存创建策略，每次成功调用 get() 都返回新对象。
 *
 *  3: 获取 public 无参构造器后，若不满足 Lambda 的使用条件，或准备 Lambda Supplier
 *     时抛出非 Error 异常，则回退到普通反射；Error 直接抛出。
 *     构造器执行失败时不回退、不重试。
 *
 *  4: 遵守 Java 访问检查，不通过 setAccessible(true) 绕过构造器的访问限制。
 *
 *  5: 构造器执行时抛出的异常（含 Error）：Lambda 路径原样抛出，
 *     包括 get() 未声明的受检异常；反射路径包装为
 *     RuntimeException -> InvocationTargetException -> 原异常。
 * </pre>
 */
public class InstanceUtil {

    // Lambda 工厂方法的类型为 ()Supplier，Supplier.get() 擦除后的方法类型为 ()Object。
    static final MethodType METHOD_TYPE_SUPPLIER = MethodType.methodType(Supplier.class);
    static final MethodType METHOD_TYPE_OBJECT = MethodType.methodType(Object.class);

    static final ComputeCache<Class<?>, Supplier<?>> CACHE = new ComputeCache<>(512);
    static final ComputeCache<Class<?>, Supplier<?>> REFLECTION_CACHE = new ComputeCache<>(512);

    static volatile boolean jit = true;

    /**
     * 设置所有工厂实例共用的创建策略开关，默认值为 true。
     * true 优先使用 Lambda，false 仅使用普通反射。
     * 切换后，后续 get() 调用使用对应缓存；不影响 JVM 自身的 JIT 编译。
     */
    public static void setJit(boolean jit) {
        InstanceUtil.jit = jit;
    }

    /**
     * 通过对工厂可访问的 public 无参构造器创建新对象。
     * 构造器异常的传播方式取决于实际创建路径，详见类说明。
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> type) {
        if (jit) {
            return ((Supplier<T>) CACHE.computeIfAbsent(type, InstanceUtil::createSupplier)).get();
        } else {
            return ((Supplier<T>) REFLECTION_CACHE.computeIfAbsent(type, InstanceUtil::createReflectionSupplier)).get();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Supplier<T> createSupplier(Class<T> type) {
        // 两种创建路径都需要 public 无参构造器；查找失败直接抛出，不进入 Lambda 回退逻辑。
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
                // invokeExact 要求调用点返回类型精确匹配 Supplier，强转必须直接作用于调用表达式。
                // 若先按 Object 接收再强转，会抛出 WrongMethodTypeException 并回退到反射。
                return (Supplier<T>) callSite.getTarget().invokeExact();
            }

        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            // 放弃本次 Lambda 优化，改用下面的反射策略；此时尚未执行目标构造器。
        }

        // 构造器延迟到 Supplier.get() 时执行，执行失败不会再次进入上面的回退逻辑。
        return () -> newInstance(constructor);
    }

    /**
     * 查找 public 无参构造器，不修改访问权限。
     * 构造器为 public 不代表其声明类对工厂可访问，调用权限仍需后续检查。
     */
    private static <T> Constructor<T> getConstructor(Class<T> type) {
        try {
            return type.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No public no-argument constructor found for " + type.getName(), e);
        }
    }

    private static boolean isVisible(Class<?> type) throws ClassNotFoundException {
        // 生成的 Lambda 通过 InstanceUtil 的类加载器解析目标类。
        // 比较 Class 对象身份，避免误用其他类加载器定义的同名类；此处不检查成员访问权限。
        // initialize=false，避免检查可见性时触发目标类初始化。
        return Class.forName(type.getName(), false, InstanceUtil.class.getClassLoader()) == type;
    }

    private static <T> Supplier<T> createReflectionSupplier(Class<T> type) {
        Constructor<T> constructor = getConstructor(type);
        return () -> newInstance(constructor);
    }

    // 保持静态，让缓存的反射 Supplier 只捕获 Constructor，避免长期持有工厂实例。
    private static <T> T newInstance(Constructor<T> constructor) {
        try {
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create instance of " + constructor.getDeclaringClass().getName(), e);
        }
    }
}


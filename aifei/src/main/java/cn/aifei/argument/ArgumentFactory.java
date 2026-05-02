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

package cn.aifei.argument;

import cn.aifei.core.Input;
import cn.aifei.core.Output;
import cn.aifei.argument.BasicArguments.*;
import cn.aifei.log.Log;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;

/**
 * ArgumentFactory
 */
public class ArgumentFactory {

    // registry 注册表支持已注册的具体类型
    protected Map<Class<?>, Class<? extends Argument<?, ?, ?>>> registry = new HashMap<>();

    // InputArgument 支持 Input 及其子类
    protected Function<Parameter, Argument<?, ?, ?>> inputArgumentFun = p -> new InputArgument();

    // OutputArgument 支持 Output 及其子类
    protected Function<Parameter, Argument<?, ?, ?>> outputArgumentFun = p -> new OutputArgument();

    // ArrayArgument 支持数组与可变参数
    protected Function<Parameter, Argument<?, ?, ?>> arrayArgumentFun = p -> new ArrayArgument<>();

    // EnumArgument 支持任意枚举类型
    protected Function<Parameter, Argument<?, ?, ?>> enumArgumentFun = p -> new EnumArgument<>();

    // BeanArgumentFun 兜底
    protected Function<Parameter, Argument<?, ?, ?>> beanArgumentFun = p -> new BeanArgument<>();

    public ArgumentFactory() {
        init();
    }

    /**
     * int[]、long[]、double[]、boolean[] 必须独立实现，其它类型数组在 ArrayArgument 中实现
     */
    protected void init() {
        register(String.class, StrArgument.class);
        register(int.class, IntArgument.class);
        register(long.class, LongArgument.class);
        register(double.class, DoubleArgument.class);
        register(boolean.class, BooleanArgument.class);
        register(Integer.class, IntArgument.class);
        register(Long.class, LongArgument.class);
        register(Double.class, DoubleArgument.class);
        register(Boolean.class, BooleanArgument.class);
        register(int[].class, IntArrayArgument.class);
        register(long[].class, LongArrayArgument.class);
        register(double[].class, DoubleArrayArgument.class);
        register(boolean[].class, BooleanArrayArgument.class);

        register(BigDecimal.class, BigDecimalArgument.class);

        register(Date.class, DateArgument.class);
        register(LocalDate.class, LocalDateArgument.class);
        register(LocalTime.class, LocalTimeArgument.class);
        register(LocalDateTime.class, LocalDateTimeArgument.class);

        register(List.class, ListArgument.class);
        register(ArrayList.class, ListArgument.class);

        register(Map.class, MapArgument.class);
        // register(HashMap.class, MapArgument.class);          // 限定实现支持 HashMap
        // register(LinkedHashMap.class, MapArgument.class);    // 限定实现支持 LinkedHashMap
    }

    /**
     * 注册 Argument
     */
    public void register(Class<?> type, Class<? extends Argument<?, ?, ?>> argument) {
        Class<? extends Argument<?, ?, ?>> prev = registry.put(type, argument);
        if (prev != null) {
            Log.get(ArgumentFactory.class).info(prev.getName() + " replaced by " + argument.getName());
        }
    }

    public void remove(Class<?> type) {
        registry.remove(type);
    }

    public void registerInputArgumentFun(Function<Parameter, Argument<?, ?, ?>> inputArgumentFun) {
        Objects.requireNonNull(inputArgumentFun, "inputArgumentFun can not be null.");
        this.inputArgumentFun = inputArgumentFun;
    }

    public void registerOutputArgumentFun(Function<Parameter, Argument<?, ?, ?>> outputArgumentFun) {
        Objects.requireNonNull(outputArgumentFun, "outputArgumentFun can not be null.");
        this.outputArgumentFun = outputArgumentFun;
    }

    public void registerArrayArgumentFun(Function<Parameter, Argument<?, ?, ?>> arrayArgumentFun) {
        Objects.requireNonNull(arrayArgumentFun, "arrayArgumentFun can not be null.");
        this.arrayArgumentFun = arrayArgumentFun;
    }

    public void registerEnumArgumentFun(Function<Parameter, Argument<?, ?, ?>> enumArgumentFun) {
        Objects.requireNonNull(enumArgumentFun, "enumArgumentFun can not be null.");
        this.enumArgumentFun = enumArgumentFun;
    }

    public void registerBeanArgumentFun(Function<Parameter, Argument<?, ?, ?>> beanArgumentFun) {
        Objects.requireNonNull(beanArgumentFun, "beanArgumentFun can not be null.");
        this.beanArgumentFun = beanArgumentFun;
    }

    /**
     * 创建 Argument 对象
     */
    public Argument<?, ?, ?> get(Parameter parameter) {
        Class<?> type = parameter.getType();
        Class<? extends Argument<?, ?, ?>> ret = registry.get(type);
        if (ret != null) {
            try {
                return ret.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        if (Input.class.isAssignableFrom(type)) {
            return inputArgumentFun.apply(parameter);
        } else if (Output.class.isAssignableFrom(type)) {
            return outputArgumentFun.apply(parameter);
        } else if (type.isArray()) {
            return arrayArgumentFun.apply(parameter);
        } else if (Enum.class.isAssignableFrom(type)) {
            return enumArgumentFun.apply(parameter);
        } else {
            return beanArgumentFun.apply(parameter);
        }
    }
}




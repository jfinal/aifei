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

import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.function.Function;

/**
 * ArgumentKit
 */
public class ArgumentKit {

    ArgumentFactory factory = new ArgumentFactory();

    private ArgumentKit() {}
    static ArgumentKit kit = new ArgumentKit();
    public static ArgumentKit get() {return kit;}

    /**
     * 创建 Argument 对象
     */
    public Argument<?, ?, ?> createArgument(Parameter parameter) {
        return factory.get(parameter);
    }

    /**
     * 注册 Argument 实现
     */
    public ArgumentKit register(Class<?> type, Class<? extends Argument<?, ?, ?>> argument) {
        factory.register(type, argument);
        return this;
    }

    /**
     * 移除 Argument 实现
     */
    public ArgumentKit remove(Class<?> type) {
        factory.remove(type);
        return this;
    }

    /**
     * 注册 Input 类型的 Argument 处理函数，接管 Input 参数注入
     */
    public ArgumentKit registerInputArgumentFun(Function<Parameter, Argument<?, ?, ?>> inputArgumentFun) {
        factory.registerInputArgumentFun(inputArgumentFun);
        return this;
    }

    /**
     * 注册 Output 类型的 Argument 处理函数，接管 Output 参数注入
     */
    public ArgumentKit registerOutputArgumentFun(Function<Parameter, Argument<?, ?, ?>> outputArgumentFun) {
        factory.registerOutputArgumentFun(outputArgumentFun);
        return this;
    }

    /**
     * 注册数组类型的 Argument 处理函数，接管数组参数注入
     */
    public ArgumentKit registerArrayArgumentFun(Function<Parameter, Argument<?, ?, ?>> arrayArgumentFun) {
        factory.registerArrayArgumentFun(arrayArgumentFun);
        return this;
    }

    /**
     * 注册 Enum 类型的 Argument 处理函数，接管任意枚举参数注入
     */
    public ArgumentKit registerEnumArgumentFun(Function<Parameter, Argument<?, ?, ?>> enumArgumentFun) {
        factory.registerEnumArgumentFun(enumArgumentFun);
        return this;
    }

    /**
     * 注册 Bean 的 Argument 处理函数，兜底参数注入，即其它所有 Argument 实现类都无法处理时使用 BeanArgument 处理
     */
    public ArgumentKit registerBeanArgumentFun(Function<Parameter, Argument<?, ?, ?>> beanArgumentFun) {
        factory.registerBeanArgumentFun(beanArgumentFun);
        return this;
    }

    /**
     * 配置 ArgumentFactory 实现替代 Aifei 官方实现，实现更深入的定制
     */
    public ArgumentKit setFactory(ArgumentFactory argumentFactory) {
        Objects.requireNonNull(argumentFactory, "argumentFactory can not be null.");
        this.factory = argumentFactory;
        return this;
    }

    /**
     * 获取 ArgumentFactory 对象
     */
    public ArgumentFactory getFactory() {
        return factory;
    }
}






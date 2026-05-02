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

package cn.aifei.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Clear 注解可用于清除 Routes、全局、Class 这三个层级上的拦截器。
 *
 * <pre>
 * Clear 设计：
 *   1: 拦截器从上到下分为 Routes、全局、Class、Method 四个层级。
 *   2: Clear 注解可放在 Class 与 Method 这两个地方。
 *   3: Clear 放在 Class 上时，可清除 Routes、全局这两个层级上的拦截器。
 *   4: Clear 放在 Method 上时，可清除Class、Routes、全局这三个层级上的拦截器。
 *   5: Clear 不带参时将清除相应层级上的全部拦截器。
 *   6: Clear 带参时将清除相应层级上的参数指定的拦截器。
 *
 * 注：Clear 使用规则与 jfinal 保持一致，无学习成本
 * </pre>
 *
 * <pre>
 * 用在 Class 上的例子:
 *    // 清除全局、Routes 这两个层级上的全部拦截器
 *    \@Clear
 *    public class UserService {}
 *
 *    // 清除全局、Routes 这两个层级上的 Aaa、Bbb 拦截器
 *    \@Clear({Aaa.class, Bbb.class})
 *    public class UserService {}
 *
 * ----------------------------------------------------------------------------
 *
 * 用在 Method 上的例子:
 *    // 清除 Class、全局、Routes 这三个层级上的全部拦截器
 *    \@Clear
 *    public void method(...) {}
 *
 *    // 清除 Class、全局、Routes 这三个层级上的 Aaa、Bbb 拦截器
 *    \@Clear({Aaa.class, Bbb.class})
 *    public void method(...) {}
 * </pre>
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Clear {
    Class<? extends Interceptor>[] value() default {};
}


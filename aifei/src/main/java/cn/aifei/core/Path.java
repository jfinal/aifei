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

package cn.aifei.core;

import java.lang.annotation.*;

/**
 * Path 注解用于配置访问路径.
 *
 * <pre>
 * Path 注解设计：
 *  1: 用于 class，method 两级。
 *  2: 用于 class 级，必须以 "/" 打头。
 *  3: 用于 method 级，以 "/" 打头为绝对路径，将忽略 class 级的配置。
 *  4: 用于 method 级，可以配置为空字符串 ""，相当于 jfinal 中约定方法名为 index 的用法，但更优雅。
 *  5: 不支持继承，提升确定性与安全性
 * </pre>
 */
// @Inherited	// 不支持继承
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Path {
	String value();
}

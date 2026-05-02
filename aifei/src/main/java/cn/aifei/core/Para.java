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
 * Para 配置 action 各项参数值
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Para {

    /**
     * 特殊 "哨兵" 值，用于表达未配置
     */
    String UNSET = "\u0000";

    /**
     * 是否为路径参数 path parameter
     */
    boolean path() default false;

    /**
     * 参数名，可配置为 ""
     */
    String name() default UNSET;

    /*
     * 与 name 互为别名，用于减少代码量，如：@Para("")。
     * 暂不支持，因为语义上可能被误认为是参数值 Para value。
     */
    // String value() default UNSET;

    /**
     * 参数默认值
     */
    String defaultValue() default UNSET;

    /**
     * 指定参数是否参与路由匹配。默认值为 true。
     *
     * <pre>
     * 使用细则：
     *  1: 同一 path 对应多个 action 时配置才有效，否则无效。以下将这种情况简称为 "一对多"，其它情况简称 "一对一"。
     *  2: "一对多" 条件下，为确保路由匹配的正确性，故将匹配参数名称、参数数量。
     *  3: "一对一" 条件下，不可能出现路由匹配错误，故不匹配参数名称、参数数量。
     *  4: 再次强调：仅 "一对多" 有意义。
     *
     * 例子：
     *   \@Path("/blog")
     *   public BlogService {
     *       public void update(int id, String title) {
     *           // 省略 ...
     *       }
     *
     *       public void update(Blog blog, @Para(match=false) User user) {
     *           // 省略 ...
     *       }
     *   }
     *
     *   上例出现了 "一对多"，即 "/blog/update" 对应到两个 action 方法，若期望调用到下方带 User 参数的
     *   action，则要求客户端请求必须提供 user 参数，这时可通过 match = false 指定该参数不参与路由匹配，
     *   从而客户端在不提供 user 参数时也能调到该 action。（当然，如果客户端确定提供 user 参数则无需该配置）
     * </pre>
     */
    boolean match() default true;
}


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

package cn.aifei.config;

import cn.aifei.aop.Interceptor;
import cn.aifei.router.RouterKit;
import java.util.function.Predicate;

/**
 * Routes
 *
 * <pre>
 *  Routes 设计：
 *  1: scan 自动扫描路由，add 手动添加路由。
 *
 *  2: 多次调用 Routes.scan(...) 可指定不同的 Routes 级拦截器。同一 target 被多次扫描到以第一次扫描到为准。
 *
 *  3: Routes 级拦截器优先级最高。相当于实现了 jfinal 中控制层全局拦截器的功能。
 *     注意：aifei 全局拦截器已统一，不再区分为控制层全局拦截器和业务层全局拦截器。
 *
 *  4: add 手动添加路由场景：对性能有极致要求，或者路由极少但 Class 文件却极多。
 * </pre>
 */
public class Routes {

    /**
     * 配置 action 重载，默认值为 false，性能最高。
     * 允许 action 重载时，aifei 将通过参数匹配结果来确定最终 action。
     */
    public Routes setActionOverload(boolean actionOverload) {
        RouterKit.get().getRouter().setActionOverload(actionOverload);
        return this;
    }

    /**
     * 手动添加路由。
     * 应用场景：对性能有极致要求，或者路由极少但 Class 文件却极多。
     */
    public Routes add(String path, Class<?> target) {
        RouterKit.get().getRouter().add(path, target, null);
        return this;
    }

    /**
     * 手动添加路由，为扫描到的路由添加 Routes 级别拦截器，Routes 拦截器调用优先级高于全局拦截器。
     * 应用场景：对性能有极致要求，或者路由极少但 Class 文件却极多。
     */
    public Routes add(String path, Class<?> target, Interceptor... interceptors) {
        RouterKit.get().getRouter().add(path, target, interceptors);
        return this;
    }

    /**
     * 扫描路由。
     */
    public void scan(String basePackage) {
        RouterKit.get().getRouter().scan(basePackage, null, null);
    }

    /**
     * 扫描路由，为扫描到的路由添加 Routes 级别拦截器，Routes 拦截器调用优先级高于全局拦截器。
     */
    public void scan(String basePackage, Interceptor... interceptors) {
        RouterKit.get().getRouter().scan(basePackage, interceptors, null);
    }

    /**
     * 扫描路由，为扫描到的路由添加 Routes 级别拦截器，Routes 拦截器调用优先级高于全局拦截器，
     * 通过 skip 函数跳过指定 Class。
     */
    public void scan(String basePackage, Interceptor[] interceptors, Predicate<Class<?>> skip) {
        RouterKit.get().getRouter().scan(basePackage, interceptors, skip);
    }
}




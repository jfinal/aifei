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

package cn.aifei.proxy.cglib;

import cn.aifei.proxy.ProxyFactory;

/**
 * CglibProxyFactory 基于 cglib 实现 aop 代理
 *
 * <pre>
 * 配置方法：
 * public void config(Settings settings) {
 *     settings.setProxyFactory(new CglibProxyFactory());
 * }
 * </pre>
 */
public class CglibProxyFactory implements ProxyFactory {

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> target) {
        // 被 cglib 代理过的类名包含 "$$EnhancerBy"。仅需调用一次 getSuperclass() 即可
        if (target.getName().contains("$$Enhance")) {
            target = (Class<T>) target.getSuperclass();
        }
        return (T) net.sf.cglib.proxy.Enhancer.create(target, new CglibCallback());
    }
}




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

/**
 * NoProxyFactory 不实现 aop 代理，仅创建对象
 */
public class NoProxyFactory implements ProxyFactory {

    InstanceFactory instanceFactory = new InstanceFactory();

    @Override
    public <T> T get(Class<T> target) {
        return instanceFactory.get(target);
    }
}


// public class NoProxyFactory implements ProxyFactory {
//     @Override
//     public <T> T get(Class<T> target) {
//         try {
//             return target.getDeclaredConstructor().newInstance();
//             // throw new RuntimeException("Configure ProxyFactory first before using Proxy");
//         } catch (ReflectiveOperationException e) {
//             throw new RuntimeException(e);
//         }
//     }
// }



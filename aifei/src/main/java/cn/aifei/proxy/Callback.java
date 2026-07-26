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
 * Callback。代理实现交给 Invocation 的目标方法回调。
 *
 * <p>具体代理实现负责将第三方代理 API 抛出的 Throwable 归一化为 Exception，
 * Error 保持原样传播。
 */
@FunctionalInterface
public interface Callback {
    Object call(Object[] args) throws Exception;
}

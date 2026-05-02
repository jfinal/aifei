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

package cn.aifei.server;

import cn.aifei.core.Input;
import cn.aifei.core.Output;
import cn.aifei.core.Handler;

/**
 * Dispatcher.
 *
 * <pre>
 * 设计：
 *  1: 引入 Dispatcher 对上游 Server 与下游 Handler 进行解耦。
 *
 *  2: Server 持有 Dispatcher，Dispatcher 持有 Handler。
 *
 *  3: Server 调用 Dispatcher.dispatch(...) 发送请求给 Dispatcher。
 *     Dispatcher 调用 Handler.handle(...)  发送请求给 Handler。
 *
 *  4: 上游 Server 传递泛型参数 P1、P2 给 Dispatcher。Dispatcher 将
 *     P1、P2 转化或封装成泛型参数 I、O 再传递给下游 Handler。
 *
 *        Server ---P1、P2---> Dispatcher ---I、O---> Handler
 *
 *  5: Dispatcher 将 Server 与 Handler 解耦之后，基于 aifei 开发的项目
 *     可以任意切换底层 Server 实现。例如切换 undertow、tomcat、netty。
 * </pre>
 */
public interface Dispatcher<P1, P2, I extends Input, O extends Output> {

    /**
     * 初始化。至少需要持有下游 Handler 对象
     */
    void init(Handler<I, O> handler);

    /**
     * 接收 Server 请求，将 P1、P2 转化为 I、O 并调用 Handler.handle(...)
     */
    void dispatch(P1 p1, P2 p2);
}



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

package cn.aifei.server.undertow;

import cn.aifei.server.Dispatcher;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import java.util.Objects;

/**
 * UndertowHandler 连接上游 Undertow 服务器与下游 Dispatcher，
 * 下游 Dispatcher 进一步连接 Aifei Handler。
 */
public class UndertowHandler implements HttpHandler {

    static boolean handleResource = true;
    static ResourceHandler resourceHandler;

    Dispatcher<HttpServerExchange, Void, ?, ?> dispatcher;

    /**
     * 设置处理静态资源，默认值为 true。如果设置为 false，可通过在 aifei Handler 链中调用
     * UndertowHandler.getResourceHandler() 获取到 ResourceHandler 后进行处理，
     * 该用法可让 aifei 在 Handler 链中控制静态资源的处理方式
     */
    public static void setHandleResource(boolean handleResource) {
        UndertowHandler.handleResource = handleResource;
    }

    /**
     * 供外部获取 resourceHandler，方便在 aifei Handler 链条的任意节点处理静态资源
     */
    public static ResourceHandler getResourceHandler() {
        return resourceHandler;
    }

    public void setResourceManager(ResourceManager resourceManager) {
        Objects.requireNonNull(resourceManager, "resourceManager can not be null.");
        resourceHandler = new ResourceHandler(resourceManager);
    }

    public void init(Dispatcher<HttpServerExchange, Void, ?, ?> dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (handleResource) {
            // RequestPath 全局唯一
            String path = exchange.getRequestPath();
            // 处理静态资源。注意: 在 IO 线程中处理静态资源性能更高，不要 dispatch(...) 到 Worker 线程处理
            if (path.indexOf('.') != -1) {
                resourceHandler.handleRequest(exchange);
                return;
            }
        }

        // 若当前处在 IO 线程并且后续进行耗时或阻塞操作，则 dispatch 到 Worker 线程，以免阻塞 IO 线程
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);    // 切换到 Worker 线程
            return;
        }

        // 启用阻塞模式以便安全地使用输入输出流。startBlocking 仅仅是阻塞与非阻塞，与同步异步无关。
        // 异步是指调用 dispatch(...) 后切到异步 Worker 线程，是指 IO 线程与 Worker 线程的异步
        exchange.startBlocking();

        // 将请求派发给下游 Handler
        dispatcher.dispatch(exchange, null);
    }
}



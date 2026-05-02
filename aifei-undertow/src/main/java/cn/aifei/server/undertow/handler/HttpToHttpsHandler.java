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

package cn.aifei.server.undertow.handler;

import cn.aifei.server.undertow.UndertowConfig;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

/**
 * http 请求重定向到 https
 * 
 * 配置方法：
 * undertow.http.toHttps = true
 * 
 * 重定向默认使用状态码 302，可配置状态码：
 * undertow.http.toHttpsStatusCode=301
 */
public class HttpToHttpsHandler implements HttpHandler {

    protected HttpHandler next;

    protected String httpsPrefix;
    protected int statusCode;
    protected UndertowConfig config;

    public HttpToHttpsHandler(HttpHandler next, UndertowConfig undertowConfig) {
        this.next = next;
        this.config = undertowConfig;
        this.statusCode = undertowConfig.getHttpToHttpsStatusCode();
    }

    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String scheme = exchange.getRequestScheme();
        if ("http".equals(scheme)) {
            String httpsUrl = buildRedirectHttpsUrl(exchange);

            exchange.setStatusCode(statusCode);
            exchange.getResponseHeaders().put(Headers.LOCATION, httpsUrl);
            exchange.getResponseHeaders().put(Headers.CONNECTION, "close");

            // exchange.getResponseSender().close();
            exchange.endExchange();
        } else {
            next.handleRequest(exchange);
        }
    }

    protected String buildRedirectHttpsUrl(HttpServerExchange exchange) {
        if (httpsPrefix == null) {
            buildUrlPrefix(exchange);
        }

        String uri = exchange.getRequestURI();			// 不包含 queryString
        String queryString = exchange.getQueryString();
        if (queryString != null && queryString.length() > 0) {
            StringBuilder ret = new StringBuilder(httpsPrefix.length() + uri.length() + 1 + queryString.length());
            ret.append(httpsPrefix).append(uri).append('?').append(queryString);
            return ret.toString();
        } else {
            StringBuilder ret = new StringBuilder(httpsPrefix.length() + uri.length());
            ret.append(httpsPrefix).append(uri);
            return ret.toString();
        }
    }

    protected void buildUrlPrefix(HttpServerExchange exchange) {
        String ret = "https://" + exchange.getHostName();

        if (config.getSslConfig().getPort() != 443) {
            ret = ret + ":" + config.getSslConfig().getPort();
        }

        this.httpsPrefix = ret;

        /*
         * exchange.getRequestURI() 已包含 context path，所以无需处理
         * 处理反而会多出一个重复的 context path：
         * https://jfinal.com/feedback/5360
         *
        String ctx = config.getContextPath();
        if ("/".equals(ctx)) {
            this.httpsPrefix = ret;
            return ;
        }

        if (ctx.startsWith("/")) {
            ret = ret + ctx;
        } else {
            ret = ret + '/' + ctx;
        }

        if (ret.endsWith("/")) {
            ret = ret.substring(0, ret.length() - 1);
        }

        this.httpsPrefix = ret;
        */
    }
}






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

import java.util.List;
import java.util.function.Consumer;
import cn.aifei.server.Dispatcher;
import cn.aifei.server.Server;
import cn.aifei.server.undertow.handler.HttpToHttpsHandler;
import cn.aifei.server.undertow.resource.ResourceManagerBuilder;
import cn.aifei.server.undertow.ssl.SslBuilder;
import cn.aifei.server.undertow.util.IpUtil;
import cn.aifei.util.StrUtil;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.UndertowOptions;
import io.undertow.Version;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.SetHeaderHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.server.handlers.resource.ResourceManager;
import static cn.aifei.server.undertow.UndertowConfig.*;

/*
 * UndertowServer
 *
 * 官方示例：https://github.com/undertow-io/undertow/tree/main/examples/src/main/java/io/undertow/examples
 */
public class UndertowServer implements Server<HttpServerExchange, Void> {

    static final String version = "1.0.1";
    protected Builder builder;
    protected Undertow undertow;
    protected UndertowConfig config;
    protected Consumer<UndertowConfig> configConsumer;
    protected Consumer<Builder> onStartConsumer;
    protected volatile boolean started = false;

    protected UndertowHandler undertowHandler = new UndertowHandler();

    public UndertowServer() {
        this.config = new UndertowConfig();
    }

    public UndertowServer(String undertowConfig) {
        this.config = new UndertowConfig(undertowConfig);
    }

    /**
     * 定制 UndertowHandler 实现
     */
    public UndertowServer setUndertowHandler(UndertowHandler undertowHandler) {
        this.undertowHandler = undertowHandler;
        return this;
    }

    /**
     * config 用于配置 UndertowServer
     *
     * <pre>
     * 例子：
     *   new UndertowServer().config(uc -> {
     *       uc.setPort(8000);
     *       uc.setGzipEnable(true);
     *       uc.setServerName("Aifei");
     *   });
     * </pre>
     */
    public UndertowServer config(Consumer<UndertowConfig> configConsumer) {
        this.configConsumer = configConsumer;
        return this;
    }

    public UndertowConfig getUndertowConfig() {
        return config;
    }

    /**
     * 启动前回调，使用 io.undertow.Undertow.Builder 对象对 Undertow 服务器进行深度配置
     *
     * <pre>
     * 例子：
     *   // 在 HTTP 1.1中，连接默认为 KEEP ALIVE，可通过如下配置去除。
     *   // 注意在每个响应中添加 "Connection: keep-alive" 纯属浪费。
     *   new UndertowServer().onStart(builder -> {
     *       builder.setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false);
     *   });
     * </pre>
     */
    public UndertowServer onStart(Consumer<Builder> onStartConsumer) {
        this.onStartConsumer = onStartConsumer;
        return this;
    }

    @Override
    public void init(Dispatcher<HttpServerExchange, Void, ?, ?> dispatcher) {
        undertowHandler.init(dispatcher);
    }

    @Override
    public synchronized void start() {
        if (started) {
            return;
        }

        try {
            // System.out.println("Starting Undertow Server on port: " + config.getPort());
            System.out.println("INFO: aifei-undertow " + version + ", undertow " + Version.getVersionString() + ", JVM " + System.getProperty("java.version"));
            doStart();
            if (config.isPrintServerUrls()) {
                printServerUrls();
            }
            started = true;

        } catch (Exception e) {
            e.printStackTrace(System.err);
            // 支持在 doStart() 中抛出异常后退出 JVM，例如端口被占用，否则在 linux 控制台 JVM 并不会退出
            System.exit(1);
        }
    }

    protected void printServerUrls() {
        String msg = "Server running at\n";
        msg += " > Local:   http://localhost:" + config.getPort();
        if (config.isSslEnable()) {
            msg += "   https://localhost:" + config.getSslConfig().getPort();
        }
        msg += "\n";

        // host 配置为 "0.0.0.0"、"::" 或者具体的 ip 地址才允许网络访问
        String host = config.getHost() != null ? config.getHost().trim() : "0.0.0.0";
        if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
            System.out.print(msg);
            return;
        }

        List<String> ipList = IpUtil.getLocalIp();
        for (String ip : ipList) {
            msg += " > Network: http://" + ip + ":" + config.getPort();
            if (config.isSslEnable()) {
                msg += "   https://" + ip + ":" + config.getSslConfig().getPort();
            }
            msg += "\n";
        }
        System.out.print(msg);
    }

    protected void doStart() {
        if (configConsumer != null) {
            configConsumer.accept(config);
            configConsumer = null;			// 配置在整个生命周期只能调用一次
        }

        // 加载命令行参数，支持 5 个核心参数
        loadCommandLineParameter();

        // 创建 Builder 对象，为 Undertow 对象的一系列配置做准备
        builder = Undertow.builder();

        // 调用各配置分项
        configUndertow();
        configHandler();

        // 在启动前进行更多配置，如通过 setServerOption 配置更多选项
        if (onStartConsumer != null) {
            onStartConsumer.accept(builder);
        }

        // 由 Builder 构建 Undertow 对象并启动
        undertow = builder.build();
        undertow.start();
        started = true;
    }

    /**
     * 使用 System.getProperty(...) 加载命令行传入的参数，覆盖配置文件中相关配置，
     * 支持五个常用参数: port、host、resourcePath、ioThreads、workerThreads
     *
     * <pre>
     *  使用示例：
     *   java -Dport=8080 -Dhost=localhost -jar aifei-vip.jar
     *
     *  传参注意事项：
     *   1：传参规则由 java 命令行给定，与 aifei-undertow 项目完全无关
     *   2：传参以 "-D" 为前缀，并且该前缀与后方的参数名之间不能有空格
     *   3：参数名与参数值中间用等号字符分格，且等号前后不能空格
     * </pre>
     */
    protected void loadCommandLineParameter() {
        String port = getSystemProperty("port", PORT);
        String host = getSystemProperty("host", HOST);
        String resourcePath = getSystemProperty("resourcePath", RESOURCE_PATH);
        String ioThreads = getSystemProperty("ioThreads", IO_THREADS);
        String workerThreads = getSystemProperty("workerThreads", WORKER_THREADS);

        if (StrUtil.notBlank(port)) {
            config.port = Integer.parseInt(port.trim());
        }
        if (StrUtil.notBlank(host)) {
            config.host = host.trim();
        }
        if (StrUtil.notBlank(resourcePath)) {
            config.resourcePath = resourcePath.trim();
        }
        if (StrUtil.notBlank(ioThreads)) {
            config.ioThreads = Integer.parseInt(ioThreads.trim());
        }
        if (StrUtil.notBlank(workerThreads)) {
            config.workerThreads = Integer.parseInt(workerThreads.trim());
        }
    }

    // 使用短变量名: port、host、resourcePath、ioThreads、workerThreads
    // 兼容带 "undertow." 前缀的长变量名，如: undertow.port
    private String getSystemProperty(String shortName, String longName) {
        String ret = System.getProperty(shortName);
        return StrUtil.notBlank(ret) ? ret : System.getProperty(longName);
    }

    protected void configUndertow() {
        // url 支持特殊字符，例如: '{' 与 '}'
        builder.setServerOption(UndertowOptions.ALLOW_UNESCAPED_CHARACTERS_IN_URL, true);

        // 配置 https
        if (config.isSslEnable()) {
            if (config.getHttp2Enable() != null && config.getHttp2Enable()) {
                builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
            }
        }

        // 配置 IO 线程与 Worker 线程
        if (config.getIoThreads() != null) {
            builder.setIoThreads(config.getIoThreads());
        }
        if (config.getWorkerThreads() != null) {
            builder.setWorkerThreads(config.getWorkerThreads());
        }

        // 配置 BufferSize 与 DirectBuffers
        if (config.getBufferSize() != null) {
            builder.setBufferSize(config.getBufferSize());
        }
        if (config.getDirectBuffers() != null) {
            builder.setDirectBuffers(config.getDirectBuffers());
        }

        // 为 UndertowHandler 配置 ResourceManager 处理静态资源
        ResourceManagerBuilder builder = new ResourceManagerBuilder(config.getResourcePath(), config.getClassLoader());
        ResourceManager resourceManager = builder.build();
        undertowHandler.setResourceManager(resourceManager);
    }

    protected void configHandler() {
        HttpHandler handler = this.undertowHandler;
        handler = configGzip(handler);
        handler = configServerName(handler);
        handler = configSsl(handler);

        if (config.isSslEnable() && config.isHttpDisable()) {
            // 开启 ssl 并且 undertow.http.disable = true 时不开启用于 http 的端口
        } else {
            builder.addHttpListener(config.getPort(), config.getHost());
        }

        // builder.setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false)
        builder.setHandler(handler);
    }

    protected HttpHandler configSsl(HttpHandler httpHandler) {
        if (config.isSslEnable()) {
            new SslBuilder(builder, config).build();
            if (config.isHttpToHttps()) {
                httpHandler = new HttpToHttpsHandler(httpHandler, config);
            }
        } else {
            if (config.isHttpToHttps()) {
                System.err.println("http redirect to https needs ssl support");
            }
        }

        return httpHandler;
    }

    protected HttpHandler configGzip(HttpHandler pathHandler) {
        if (config.isGzipEnable()) {
            ContentEncodingRepository repository = new ContentEncodingRepository();
            GzipEncodingProvider provider = new GzipEncodingProvider(config.getGzipLevel());
            int minLength = config.getGzipMinLength();
            Predicate predicate = minLength > 0 ? Predicates.requestLargerThan(minLength) : Predicates.truePredicate();
            repository.addEncodingHandler("gzip", provider, 100, predicate);
            return new EncodingHandler(pathHandler, repository);
        }
        return pathHandler;
    }

    protected HttpHandler configServerName(HttpHandler pathHandler) {
        String serverName = config.getServerName();
        if (serverName != null) {
            return new SetHeaderHandler(pathHandler, "Server", serverName);
        } else {
            return pathHandler;
        }
    }

    @Override
    public synchronized void stop() {
        if (started) {
            undertow.stop();
            started = false;
        }
    }
}


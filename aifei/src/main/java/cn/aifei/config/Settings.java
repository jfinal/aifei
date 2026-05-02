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

import cn.aifei.aop.AopKit;
import cn.aifei.aop.Interceptor;
import cn.aifei.aop.InterceptorKit;
import cn.aifei.core.Handler;
import cn.aifei.core.Input;
import cn.aifei.core.Output;
import cn.aifei.argument.ArgumentKit;
import cn.aifei.log.LogFactory;
import cn.aifei.log.LogKit;
import cn.aifei.proxy.ProxyFactory;
import cn.aifei.proxy.ProxyKit;
import cn.aifei.server.Dispatcher;
import cn.aifei.server.Server;
import cn.aifei.util.StrUtil;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Settings
 */
public class Settings<I extends Input, O extends Output> {

    Server<?, ?> server;
    Dispatcher<?, ?, I, O> dispatcher;
    List<Handler<I, O>> handlerList = new ArrayList<>(3);

    Path uploadPath = Paths.get("upload");
    Path downloadPath = Paths.get("download");

    /**
     * 配置日志工厂
     * <pre>
     * 例子：
     *    // 直接使用 log4j2 日志
     *    settings.setLogFactory(new Log4j2LogFactory());
     *
     *    // 使用 slf4j 日志，可接入任意实现了 slf4j 规范的日志系统
     *    settings.setLogFactory(new Slf4jLogFactory());
     * </pre>
     */
    public void setLogFactory(LogFactory logFactory) {
        LogKit.get().setLogFactory(logFactory);
    }

    /**
     * 配置 Server 及其配套的 Dispatcher。
     * 泛型 P1、P2 保障 Server 与 Dispatcher 之间传递的数据类型一致。
     * 泛型 I、O 保障 Dispatcher 与 Settings 类的数据类型一致，从而保障与 AifeiConfig 实现类的数据类型一致。
     */
    public <P1, P2> Settings<I, O> setServer(Server<P1, P2> server, Dispatcher<P1, P2, I, O> dispatcher) {
        Objects.requireNonNull(server, "server can not be null.");
        Objects.requireNonNull(dispatcher, "dispatcher can not be null.");
        server.init(dispatcher);
        this.server = server;
        this.dispatcher = dispatcher;
        return this;
    }

    /**
     * 添加 Handler。
     * 系统至少需要添加一个 Handler，Server 接收到的请求先传递给 Dispatcher，
     * 再由 Dispatcher 传递给此处添加的 Handler。
     */
    public Settings<I, O> addHandler(Handler<I, O> handler) {
        Objects.requireNonNull(handler, "handler can not be null.");
        handlerList.add(handler);
        return this;
    }

    /**
     * 添加全局拦截器。
     *
     * 注意：Aifei 只有一种全局拦截器，而非像 jfinal 区分了控制层全局拦截器与业务层全局拦截器。
     */
    public Settings<I, O> addGlobalInterceptor(Interceptor globalInterceptor) {
        Objects.requireNonNull(globalInterceptor, "globalInterceptor can not be null.");
        InterceptorKit.get().addGlobalServiceInterceptor(globalInterceptor);
        return this;
    }

    /**
     * 配置 Argument
     *
     * <pre>
     * 例子：
     *    settings.configArgument(kit -> {
     *        kit.register(OutputStream.class, OutputStreamArgument.class);
     *        kit.registerBeanArgumentFun(new MyBeanArgumentFun());
     *    });
     * </pre>
     */
    public void configArgument(Consumer<ArgumentKit> kit) {
        kit.accept(ArgumentKit.get());
    }

    /**
     * 配置代理工厂，业务层 AOP 必须配置代理工厂，否则只注入纯实例对象。
     */
    public void setProxyFactory(ProxyFactory proxyFactory) {
        ProxyKit.get().setProxyFactory(proxyFactory);
    }

    /**
     * 配置超类是否注入，默认 false
     */
    public void setInjectSuperClass(boolean injectSuperClass) {
        AopKit.get().setInjectSuperClass(injectSuperClass);
    }

    /**
     * 配置文件上传路径，默认值为 "upload"。
     *
     * <pre>
     * 上传路径配置设计：
     *  1: 不以字符 '/' 打头的配置为相对路径，是指相对于 webRootPath 的路径，通常是指相对于 webapp 目录的路径。
     *  2: 以字符 '/' 打头的配置为绝对路径，便于配置到项目之外的路径。
     *  3: 支持使用 ".." 配置为 webRootPath 的父路径，例如 "../upload" 将上传路径配置为与 webapp 平级。
     *  注: Windows 系统绝对路径以 "盘符" 打头，上述中的字符 '/' 仅针对 Windows 以外的系统。
     *
     * 例子：
     *  // 配置上传路径与 webapp 处理同一级目录，一般用于部署环境
     *  settings.setUploadPath("../upload");
     *
     *  // 配置上传路径在 webapp 的上一级目录
     *  settings.setUploadPath("../../upload");
     * </pre>
     */
    public void setUploadPath(String uploadPath) {
        StrUtil.requireNotBlank(uploadPath, "uploadPath can not be blank.");
        this.uploadPath = Paths.get(uploadPath);
    }

    /**
     * 配置文件下载路径，默认值为 "download"。
     *
     * <pre>
     * 下载路径配置设计：
     *  1: 不以字符 '/' 打头的配置为相对路径，是指相对于 webRootPath 的路径，通常是指相对于 webapp 目录的路径。
     *  2: 以字符 '/' 打头的配置为绝对路径，便于配置到项目之外的路径。
     *  3: 支持使用 ".." 配置为 webRootPath 的父路径，例如 "../download" 将下载路径配置为与 webapp 平级。
     *  注: Windows 系统绝对路径以 "盘符" 打头，上述中的字符 '/' 仅针对 Windows 以外的系统。
     *
     * 例子：
     *  // 配置下载路径与 webapp 处理同一级目录，一般用于部署环境
     *  settings.setDownloadPath("../download");
     *
     *  // 配置下载路径在 webapp 的上一级目录
     *  settings.setDownloadPath("../../download");
     * </pre>
     */
    public void setDownloadPath(String downloadPath) {
        StrUtil.requireNotBlank(downloadPath, "downloadPath can not be blank.");
        this.downloadPath = Paths.get(downloadPath);
    }

    // ---------------------------------------------------------------------------------------------------------

    // 仅 Aifei 内部使用
    public Server<?, ?> getServer() {
        return server;
    }

    // 仅 Aifei 内部使用
    public Dispatcher<?, ?, I, O> getDispatcher() {
        return dispatcher;
    }

    // 仅 Aifei 内部使用
    public List<Handler<I, O>> getHandlerList() {
        return handlerList;
    }

    // 仅 Aifei 内部使用。获取 uploadPath 需使用 PathUtil.getUploadPath()
    public Path getUploadPath() {
        return uploadPath;
    }

    // 仅 Aifei 内部使用。获取 downloadPath 需使用 PathUtil.getDownloadPath()
    public Path getDownloadPath() {
        return downloadPath;
    }
}




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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import cn.aifei.core.Aifei;
import cn.aifei.server.undertow.ssl.SslConfig;
import cn.aifei.util.PathUtil;
import cn.aifei.util.Prop;
import cn.aifei.util.PropKit;
import cn.aifei.util.StrUtil;
import io.undertow.util.StatusCodes;

/**
 * UndertowConfig
 */
public class UndertowConfig {

    static final String UNDERTOW_CONFIG                 = "undertow.txt";

    static final String PORT                            = "undertow.port";
    static final String HOST                            = "undertow.host";

    static final String RESOURCE_PATH                   = "undertow.resourcePath";
    static final String ALLOW_RESOURCE_CHANGE_LISTENERS = "undertow.allowResourceChangeListeners";

    static final String IO_THREADS                      = "undertow.ioThreads";
    static final String WORKER_THREADS                  = "undertow.workerThreads";

    static final String GZIP_ENABLE                     = "undertow.gzip.enable";
    static final String GZIP_LEVEL                      = "undertow.gzip.level";
    static final String GZIP_MIN_LENGTH                 = "undertow.gzip.minLength";

    static final String HTTP2_ENABLE                    = "undertow.http2.enable";

    // ssl 模式下 http 请求是否跳转到 https
    static final String HTTP_TO_HTTPS                   = "undertow.http.toHttps";
    // ssl 模式下 http 请求跳转到 https 使用的状态码
    static final String HTTP_TO_HTTPS_STATUS_CODE       = "undertow.http.toHttpsStatusCode";
    // ssl 模式下是否关闭 http
    static final String HTTP_DISABLE                    = "undertow.http.disable";

    static final String SERVER_NAME                     = "undertow.serverName";

    static final String BUFFER_SIZE                     = "undertow.bufferSize";
    static final String DIRECT_BUFFERS                  = "undertow.directBuffers";

    static final String PRINT_SERVER_URLS               = "undertow.printServerUrls";

    static final String MAX_BODY_SIZE                   = "undertow.maxBodySize";

    // ----------------------------------------------------------------------------

    protected int port                          = 80;
    protected String host                       = "0.0.0.0";

    protected String resourcePath               = "webapp, src/main/webapp, WebRoot, WebContent"; // web 资源路径

    protected Integer ioThreads                 = null;
    protected Integer workerThreads             = null; // ioThreads * 16;

    protected boolean gzipEnable                = false;
    protected int gzipLevel                     = Deflater.DEFAULT_COMPRESSION;
    protected int gzipMinLength                 = 1024;

    protected Boolean http2Enable               = null;

    protected SslConfig sslConfig               = null;
    protected boolean httpToHttps               = false;
    protected int httpToHttpsStatusCode         = StatusCodes.FOUND;
    protected boolean httpDisable               = false;

    protected String serverName                 = null;

    protected Integer bufferSize                = null;
    protected Boolean directBuffers             = null;

    protected boolean printServerUrls           = true;

    protected long maxBodySize                  = 2 * 1024 * 1024;

    protected ClassLoader classLoader;
    protected Prop p;

    /**
     * UndertowConfig 无参构造。
     *
     * <pre>
     *  无参构造设计：
     *   1: 若配置了 aifei.profiles.active，则将其与 "undertow.txt" 拼接后去加载配置文件。
     *      若拼接后的配置文件不存在，则抛出异常。
     *
     *   2: 若未配置 aifei.profiles.active，则尝试加载 "undertow.txt"。配置文件不存不抛异常，
     *      兼容 jfinal undertow 使用习惯。
     *
     *   3: aifei.profiles.active 支持配置多个值，例如：
     *      aifei.profiles.active = common, pro
     *      以上将加载 undertow-common.txt 与 undertow-pro.txt
     *
     *   4: 如果配置项不多，可共享项目已有配置，如 config.txt：
     *         // 在创建 UndertowServer 时传入配置文件
     *         new UndertowServer("config.txt");
     *
     *      config.txt 配置文件中的内容如下：
     *         undertow.port = 80
     * </pre>
     */
    public UndertowConfig() {
        loadProp(UNDERTOW_CONFIG, false);
        init();
    }

    /**
     * 使用指定配置构造 UndertowConfig。
     *
     * <pre>
     *  有参构造设计：
     *   1: 若配置了 aifei.profiles.active，则将其与构造参数拼接后去加载配置文件。
     *      若拼接后的配置文件不存在，则抛出异常。
     *
     *   2: 若未配置 aifei.profiles.active，则加载构造参数指向的配置文件。配置文件不存在必须抛异常，
     *      对于指定的配置要求其必须存在，提早发现潜在问题。
     *
     *   3: aifei.profiles.active 支持配置多个值，例如：
     *      aifei.profiles.active = common, pro
     *      假定构造方法指定的参数值为 "abc.txt"，以上将加载 abc-common.txt 与 abc-pro.txt
     * </pre>
     */
    public UndertowConfig(String undertowConfig) {
        loadProp(undertowConfig.trim(), true);
        init();
    }

    protected void loadProp(String config, boolean givenConfig) {
        // 获取来自 PropKit 或者其它方式传递的系统变量
        String activeProfiles = System.getProperty(PropKit.getActiveProfilesKey());
        List<String> activeProfileList;
        if (activeProfiles != null) {
            activeProfileList = Arrays.stream(activeProfiles.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        } else {
            activeProfileList = Collections.emptyList();
        }

        p = new Prop();
        if (activeProfileList.isEmpty()) {
            // 未配置 aifei.profiles.active 加载默认配置 "undertow.txt"
            // 若给定配置，则配置必须存在，使用 append 而非 appendIfExists
            if (givenConfig) {
                p.append(config);
            } else {
                p.appendIfExists(config);   // 保留 jfinal undertow 习惯
            }

        } else {
            // 配置了 aifei.profiles.active 使用 config 参数拼接上 profiles，例如："undertow-pro.txt"
            // 此分支要求配置文件必须存在，使用 append 而非 appendIfExists
            for (String activeProfile : activeProfileList) {
                String file = buildPropFileName(config, activeProfile);
                p.append(file);
            }
        }
    }

    private String buildPropFileName(String fileName, String activeProfile) {
        int index = fileName.lastIndexOf('.');
        if (index > 0) {
            String main = fileName.substring(0, index);
            String ext = fileName.substring(index);
            return main + "-" + activeProfile + ext;
        } else {
            return fileName + "-" + activeProfile;
        }
    }

    protected void init() {
        if (p.isEmpty()) {
            return;
        }

        initAllowResourceChangeListeners();

        port = p.getInt(PORT, port);
        host = p.get(HOST, host).trim();

        resourcePath = p.get(RESOURCE_PATH, resourcePath).trim();

        ioThreads = buildIoThreads();
        workerThreads = buildWorkerThreads(); // = p.getInt(WORKER_THREADS, workerThreads);

        gzipEnable = p.getBoolean(GZIP_ENABLE, gzipEnable);
        gzipLevel = checkGzipLevel(p.getInt(GZIP_LEVEL, gzipLevel));
        gzipMinLength = p.getInt(GZIP_MIN_LENGTH, gzipMinLength);

        http2Enable = p.getBoolean(HTTP2_ENABLE, http2Enable);

        sslConfig = new SslConfig(p);
        httpToHttps = p.getBoolean(HTTP_TO_HTTPS, httpToHttps);
        httpToHttpsStatusCode = p.getInt(HTTP_TO_HTTPS_STATUS_CODE, httpToHttpsStatusCode);
        httpDisable = p.getBoolean(HTTP_DISABLE, httpDisable);

        serverName = p.get(SERVER_NAME);

        bufferSize = p.getInt(BUFFER_SIZE);
        directBuffers = p.getBoolean(DIRECT_BUFFERS);

        printServerUrls = p.getBoolean(PRINT_SERVER_URLS, printServerUrls);

        maxBodySize = p.getLong(MAX_BODY_SIZE, maxBodySize);
    }

    /**
     * <pre>
     * 配置是否打开资源文件变动监听器，配置为 false 可解决项目资源文件过多所导致的启动过慢问题
     *
     * 当项目中的资源文件过多时（例如有几百万张图片），undertow 启动会非常慢，通过如下配置可以解决：
     * undertow.allowResourceChangeListeners = false
     *
     * 目前是针对 PathResourceManager.java 中的相关逻辑，通过设置 "io.undertow.disable-file-system-watcher"
     * 为 "true" 来解决的，如果将来这部分源码的逻辑有变，需要随之改变解决方案
     *
     * 对于绝大多数项目无需关注该配置
     * </pre>
     */
    private void initAllowResourceChangeListeners() {
        Boolean allowResourceChangeListeners = p.getBoolean(ALLOW_RESOURCE_CHANGE_LISTENERS);
        if (allowResourceChangeListeners != null) {
            Boolean disableFileSystemWatcher = ! allowResourceChangeListeners;
            System.setProperty("io.undertow.disable-file-system-watcher", disableFileSystemWatcher.toString());
        }
    }

    /**
     * 优先使用外部配置文件中指定的值，当外部配置没有指定并且 notDeployMode 下使用更少的线程以节省时空
     */
    protected Integer buildIoThreads() {
        Integer valueFromConfig = p.getInt(IO_THREADS);
        if (valueFromConfig != null) {
            return valueFromConfig;
        }

        int cpuNum = Runtime.getRuntime().availableProcessors();
        if (/* isDevMode() && */ PathUtil.notDeployMode()) {
            return 4;
        } else {
            // return cpuNum * 2;
            return new Double(Math.ceil(cpuNum * 1.6180339)).intValue();
            // return cpuNum + 1;
        }
    }

    protected Integer buildWorkerThreads() {
        Integer valueFromConfig = p.getInt(WORKER_THREADS);
        if (valueFromConfig != null) {
            return valueFromConfig;
        }

        if (/* isDevMode() && */ PathUtil.notDeployMode()) {
            return ioThreads * 2;
        } else {
            return ioThreads * 16;
            // return ioThreads * 8;
        }
    }

    protected int checkGzipLevel(int gzipLevel) {
        if (gzipLevel != -1 && (gzipLevel < 1 || gzipLevel > 9)) {
            throw new IllegalArgumentException(GZIP_LEVEL + " 不能配置为 " + gzipLevel + ", 可配置的值为: -1, 1, 2, 3, 4, 5, 6, 7, 8, 9");
        }
        return gzipLevel;
    }

    public void setPort(int port) {
        if (p.getInt(PORT) == null) {
            this.port = port;
        } else {
            System.out.println("undertow-server: 优先使用配置文件中的 " + PORT + " = " + p.getInt(PORT));
        }
    }

    public int getPort() {
        return port;
    }

    public void setResourcePath(String resourcePath) {
        if (StrUtil.isBlank(resourcePath)) {
            throw new IllegalArgumentException("resourcePath can not be blank");
        }
        if (p.get(RESOURCE_PATH) == null) {
            this.resourcePath = resourcePath;
        } else {
            System.out.println("undertow-server: 优先使用配置文件中的 " + RESOURCE_PATH + " = " + p.get(RESOURCE_PATH));
        }
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setHost(String host) {
        if (p.get(HOST) == null) {
            this.host = host;
        } else {
            System.out.println("undertow-server: 优先使用配置文件中的 " + HOST + " = " + p.get(HOST));
        }
    }

    public String getHost() {
        return host;
    }

    public void setIoThreads(int ioThreads) {
        if (p.getInt(IO_THREADS) == null) {
            this.ioThreads = ioThreads;
        } else {
            System.out.println("undertow-server: 优先使用配置文件中的 " + IO_THREADS + " = " + p.getInt(IO_THREADS));
        }
    }

    public Integer getIoThreads() {
        return ioThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        if (p.getInt(WORKER_THREADS) == null) {
            this.workerThreads = workerThreads;
        } else {
            System.out.println("undertow-server: 优先使用配置文件中的 " + WORKER_THREADS + " = " + p.getInt(WORKER_THREADS));
        }
    }

    public Integer getWorkerThreads() {
        return workerThreads;
    }

    public void setGzipEnable(boolean gzipEnable) {
        if (p.getBoolean(GZIP_ENABLE) == null) {
            this.gzipEnable = gzipEnable;
        } else {
            System.out.println("undertow-server: 优先使用配置文件中的 " + GZIP_ENABLE + " = " + p.getBoolean(GZIP_ENABLE));
        }
    }

    public boolean isGzipEnable() {
        return gzipEnable;
    }

    public void setGzipLevel(int gzipLevel) {
        if (p.getInt(GZIP_LEVEL) == null) {
            this.gzipLevel = checkGzipLevel(gzipLevel);
        } else {
            System.out.println("undertow-server: 优先使用配置文件中的 " + GZIP_LEVEL + " = " + p.getInt(GZIP_LEVEL));
        }
    }

    public int getGzipLevel() {
        return gzipLevel;
    }

    public void setGzipMinLength(int gzipMinLength) {
        if (p.getInt(GZIP_MIN_LENGTH) == null) {
            this.gzipMinLength = gzipMinLength;
        } else {
            System.out.println("undertow-server: 优先使用配置文件中的 " + GZIP_MIN_LENGTH + " = " + p.getInt(GZIP_MIN_LENGTH));
        }
    }

    public int getGzipMinLength() {
        return gzipMinLength;
    }

    public void setHttp2Enable(boolean http2Enable) {
        if (p.getBoolean(HTTP2_ENABLE) == null) {
            this.http2Enable = http2Enable;
        } else {
            System.out.println("undertow-server: 优先使用配置文件中的 " + HTTP2_ENABLE + " = " + p.getBoolean(HTTP2_ENABLE));
        }
    }

    public Boolean getHttp2Enable() {
        return http2Enable;
    }

    public boolean isSslEnable() {
        return sslConfig != null && sslConfig.isEnable();
    }

    public SslConfig getSslConfig() {
        return sslConfig;
    }

    public void setSslConfig(SslConfig sslConfig) {
        this.sslConfig = sslConfig;
    }

    public void setHttpToHttps(boolean httpToHttps) {
        if (p.getBoolean(HTTP_TO_HTTPS) == null) {
            this.httpToHttps = httpToHttps;
        } else {
            System.out.println("undertow-server: 优先使用配置文件中的 " + HTTP_TO_HTTPS + " = " + p.getBoolean(HTTP_TO_HTTPS));
        }
    }

    public boolean isHttpToHttps() {
        return httpToHttps;
    }

    public void setHttpToHttpsStatusCode(int httpToHttpsStatusCode) {
        if (p.getInt(HTTP_TO_HTTPS_STATUS_CODE) == null) {
            this.httpToHttpsStatusCode = httpToHttpsStatusCode;
        } else {
            System.out.println("undertow-server: 优先使用配置文件中的 " + HTTP_TO_HTTPS_STATUS_CODE + " = " + p.getInt(HTTP_TO_HTTPS_STATUS_CODE));
        }
    }

    public int getHttpToHttpsStatusCode() {
        return httpToHttpsStatusCode;
    }

    public void setHttpDisable(boolean httpDisable) {
        if (p.getBoolean(HTTP_DISABLE) == null) {
            this.httpDisable = httpDisable;
        } else {
            System.out.println("undertow-server: 优先使用配置文件中的 " + HTTP_DISABLE + " = " + p.getBoolean(HTTP_DISABLE));
        }
    }

    public boolean isHttpDisable() {
        return httpDisable;
    }

    public void setServerName(String serverName) {
        if (p.get(SERVER_NAME) == null) {
            this.serverName = serverName;
        } else {
            System.out.println("undertow-server: 优先使用配置文件中的 " + SERVER_NAME + " = " + p.get(SERVER_NAME));
        }
    }

    /**
     * 在 HTTP response header 中显示的服务名，配置为 disable 时表示不启用
     *
     * 未配置则使用默认值，例如：Aifei 4.3，否则使用配置的值
     * 注意：disable 为特殊配置，表示不启用该功能
     */
    public String getServerName() {
        if (StrUtil.isBlank(serverName)) {
            return "Aifei " + Aifei.getVersion();
        } else {
            return "disable".equals(serverName.trim()) ? null : serverName.trim();
        }
    }

    public void setBufferSize(int bufferSize) {
        if (p.getInt(BUFFER_SIZE) == null) {
            this.bufferSize = bufferSize;
        } else {
            System.out.println("undertow-server: 优先使用配置文件中的 " + BUFFER_SIZE + " = " + p.getInt(BUFFER_SIZE));
        }
    }

    public Integer getBufferSize() {
        return bufferSize;
    }

    public void setDirectBuffers(boolean directBuffers) {
        if (p.getBoolean(DIRECT_BUFFERS) == null) {
            this.directBuffers = directBuffers;
        } else {
            System.out.println("undertow-server: 优先使用配置文件中的 " + DIRECT_BUFFERS + " = " + p.getBoolean(DIRECT_BUFFERS));
        }
    }

    public Boolean getDirectBuffers() {
        return directBuffers;
    }

    public void setPrintServerUrls(boolean printServerUrls) {
        if (p.getBoolean(PRINT_SERVER_URLS) == null) {
            this.printServerUrls = printServerUrls;
        } else {
            System.out.println("undertow-server: 优先使用配置文件中的 " + PRINT_SERVER_URLS + " = " + p.getBoolean(PRINT_SERVER_URLS));
        }
    }

    public boolean isPrintServerUrls() {
        return printServerUrls;
    }

    public void setMaxBodySize(long maxBodySize) {
        if (p.getLong(MAX_BODY_SIZE) == null) {
            this.maxBodySize = maxBodySize;
        } else {
            System.out.println("undertow-server: 优先使用配置文件中的 " + MAX_BODY_SIZE + " = " + p.getLong(MAX_BODY_SIZE));
        }
    }

    public long getMaxBodySize() {
        return maxBodySize;
    }

    public void setClassLoader(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader can not be null.");
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            ClassLoader ret = Thread.currentThread().getContextClassLoader();
            classLoader = ret != null ? ret : UndertowConfig.class.getClassLoader();
        }
        return classLoader;
    }
}




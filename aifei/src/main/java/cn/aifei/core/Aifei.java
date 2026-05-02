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

package cn.aifei.core;

import cn.aifei.config.AifeiConfig;
import cn.aifei.config.Plugins;
import cn.aifei.config.Routes;
import cn.aifei.config.Settings;
import cn.aifei.log.Log;
import cn.aifei.plugin.Plugin;
import cn.aifei.util.PathUtil;
import cn.aifei.util.StrUtil;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;
import java.nio.file.Path;

/**
 * Aifei start entry.
 */
public class Aifei {

    static final String VERSION = "1.0.0";

    static Routes routes;
    static Plugins plugins;
    static Settings<?, ?> settings;
    static AifeiConfig<?, ?> aifeiConfig;

    private Aifei() {}

    public static <I extends Input, O extends Output> void start(AifeiConfig<I, O> aifeiConfig, String[] args) {
        Objects.requireNonNull(aifeiConfig, "aifeiConfig can not be null.");

        System.out.println("Starting Aifei " + VERSION);
        long startTime = System.currentTimeMillis();
        commandLineArgumentToSystemProperty(args);
        Aifei.aifeiConfig = aifeiConfig;
        PathUtil.init(aifeiConfig);

        // 创建各配置对象
        Settings<I, O> settings = new Settings<>();
        Aifei.settings = settings;
        Aifei.routes = new Routes();
        Aifei.plugins = new Plugins();

        // 回调用户配置逻辑
        aifeiConfig.config(settings);   // PathUtil.init(aifeiConfig);
        aifeiConfig.config(routes);
        aifeiConfig.config(plugins);

        // 检测 Server、Handler 是否已配置
        if (settings.getServer() == null) {
            throw new IllegalStateException("Server not configured.");
        }
        if (settings.getHandlerList().isEmpty()) {
            throw new IllegalStateException("Handler not configured.");
        }

        // 启动 Plugin
        startPlugins(plugins);

        // 回调 onStart，可使用 plugin
        aifeiConfig.onStart();

        // 创建 Handler 链
        Handler<I, O> handler = makeHandlerChain(settings.getHandlerList());

        // 初始化 Dispatcher
        settings.getDispatcher().init(handler);

        // 启动 Server
        settings.getServer().start();
        System.out.println("Started in " + getTimeSpent(startTime) + " s. Enjoy Aifei (^_^)\n");

        /*
         * 使用 kill pid 命令或者 ctrl + c 关闭 JVM 时，调用 Aifei.stop() 方法。
         * 注意：aifei 下测试成功。只支持 kill pid 不支持 kill -9 pid
         */
        Runtime.getRuntime().addShutdownHook(new Thread(Aifei::stop));
    }

    /**
     * 将来自 main 入口方法的命令行参数转换为系统属性，约定参数以 "--" 打头（以 "-D" 打头的 JVM 已经处理）
     *
     * <pre>
     * 命令行参数用法：
     *  1: 命令行参数会自动传给 main 入口方法的 String[] args 参数。
     *
     *  2: 约定参数格式以 "--" 打头。例如配置激活环境（profile）：
     *     java -jar app.jar --aifei.profiles.active=pro
     *
     *  3: 配置项没有 value 时约定为开关类型（flag），默认值为 "true"
     *     即配置项 --debug 等价于 --debug=true
     *
     *  4: 将参数存为系统变量供后续使用，如 PropKit 中获取 aifei.profiles.active 值。
     *     key = value： 存为 System.setProperty(key, value)
     *     key 存为：    存为 System.setProperty(key, "true")
     *
     * </pre>
     */
    private static void commandLineArgumentToSystemProperty(String[] args) {
        if (args != null) {
            for (String arg : args) {
                if (arg != null && arg.trim().startsWith("--")) {
                    String originalArg = arg;
                    arg = arg.trim().substring(2);

                    if (arg.contains("=")) {
                        String[] parts = arg.split("=");
                        if (parts.length != 2 || StrUtil.isBlank(parts[0]) || StrUtil.isBlank(parts[1])) {
                            throw new IllegalArgumentException("Invalid argument syntax: \"" + originalArg + "\"");
                        }
                        // 存为系统变量供后续使用。如传递 "--aifei.profiles.active=pro" 参数在 PropKit 中获取
                        System.setProperty(parts[0].trim(), parts[1].trim());

                    } else {
                        if (StrUtil.isBlank(arg)) {
                            throw new IllegalArgumentException("Invalid argument syntax: \"" + originalArg + "\"");
                        }
                        // 没有等号约定为开关类型（flag），例如 --debug
                        System.setProperty(arg.trim(), "true");
                    }
                }
            }
        }
    }

    public static void stop() {
        System.out.println("Stopping Aifei " + VERSION);
        long startTime = System.currentTimeMillis();

        // 关闭 Server
        settings.getServer().stop();
        // 回调 onStop，仍可使用 plugin
        aifeiConfig.onStop();
        // 关闭 Plugin
        stopPlugins(plugins);

        System.out.println("Stopped in " + getTimeSpent(startTime) + " s. See you later (^_^)\n");
    }

    private static String getTimeSpent(long startTime) {
        float timeSpent = (System.currentTimeMillis() - startTime) / 1000F;
        return new DecimalFormat("0.00").format(timeSpent);
    }

    private static void startPlugins(Plugins plugins) {
        for (Plugin plugin : plugins.getPluginList()) {
            try {
                plugin.start();
            } catch (Exception e) {
                Log.get(Aifei.class).error("Starting plugin error: " + e.getMessage(), e);
                throw new RuntimeException(e.getMessage(), e);  // 抛异常终止启动
            }
        }
    }

    private static void stopPlugins(Plugins plugins) {
        for (Plugin plugin : plugins.getPluginList()) {
            try {
                plugin.stop();
            } catch (Exception e) {
                Log.get(Aifei.class).error("Stopping plugin error: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 将 handlerList 通过 next 属性组装成 handler 链
     */
    private static <I extends Input, O extends Output> Handler<I, O> makeHandlerChain(List<Handler<I, O>> handlerList) {
        if (handlerList.isEmpty()) {
            throw new IllegalStateException("handlerList can not be empty.");
        }

        Handler<I, O> ret = handlerList.get(handlerList.size() - 1);
        for (int i = handlerList.size() - 2; i >= 0; i--) {
            Handler<I, O> current = handlerList.get(i);
            current.next = ret;
            ret = current;
        }
        return ret;
    }

    /**
     * 指定 appHome，仅用于 appHome 探测失败的情况。
     */
    public static void setAppHome(Path appHome) {
        PathUtil.setAppHome(appHome);
    }

    /**
     * 获取 appHome
     */
    public static Path getAppHome() {
        return PathUtil.getAppHome();
    }

    /**
     * 获取 Aifei 版本号
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     * 获取 Settings
     */
    public static Settings<?, ?> getSettings() {
        return settings;
    }
}




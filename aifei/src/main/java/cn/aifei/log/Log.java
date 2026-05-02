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

package cn.aifei.log;

import java.util.function.Supplier;

/**
 * Log.
 * 带有 Supplier 参数的方法需保留，用于如下场景：
 * log.debug(() -> "Expensive log: " + doSomething());
 *
 * <pre>
 * aifei-slf4j、aifei-log4j2 实现分别依赖 slf4j 与 log4j2，当 Log 接口
 * 无法满足需求时，可以在项目中直接使用 slf4j 与 log4j2 的 api 来实现功能，
 * 例如当需要使用 Marker 机制时。
 *
 * The five logging levels used by Log are (in order):
 * 1. TRACE (the least serious)
 * 2. DEBUG
 * 3. INFO
 * 4. WARN
 * 5. ERROR (the most serious)
 * </pre>
 */
public interface Log {

    /**
     * 无参获取日志，极速开发、极致体验。
     *
     * <pre>
     * 日志最常用的两种场景
     * 1: static 属性：
     *    static Log log = Log.get();
     *
     * 2: singleton 对象属性，如果 Service 对象是 singleton：
     *    public class Service {
     *        Log log = Log.get();
     *    }
     *
     * 无参 Log.get() 稍有性能消耗，但以上常用场景避免了消耗。
     * 其它场景建议加上 static 关键字避免性能消耗。
     * </pre>
     */
    static Log get() {
        return LogKit.logFactory.getLog();
    }

    static Log get(Class<?> clazz) {
        return LogKit.logFactory.getLog(clazz);
    }

    static Log get(String name) {
        return LogKit.logFactory.getLog(name);
    }

    String getName();

    boolean isTraceEnabled();
    void trace(String msg, Throwable t);
    void trace(String msg);
    void trace(String format, Object arg);
    void trace(String format, Object arg1, Object arg2);
    void trace(String format, Object... arguments);
    void trace(Supplier<String> messageSupplier);

    boolean isDebugEnabled();
    void debug(String msg, Throwable t);
    void debug(String msg);
    void debug(String format, Object arg);
    void debug(String format, Object arg1, Object arg2);
    void debug(String format, Object... arguments);
    void debug(Supplier<String> messageSupplier);

    boolean isInfoEnabled();
    void info(String msg, Throwable t);
    void info(String msg);
    void info(String format, Object arg);
    void info(String format, Object arg1, Object arg2);
    void info(String format, Object... arguments);
    void info(Supplier<String> messageSupplier);

    boolean isWarnEnabled();
    void warn(String msg, Throwable t);
    void warn(String msg);
    void warn(String format, Object arg);
    void warn(String format, Object arg1, Object arg2);
    void warn(String format, Object... arguments);
    void warn(Supplier<String> messageSupplier);

    boolean isErrorEnabled();
    void error(String msg, Throwable t);
    void error(String msg);
    void error(String format, Object arg);
    void error(String format, Object arg1, Object arg2);
    void error(String format, Object... arguments);
    void error(Supplier<String> messageSupplier);
}


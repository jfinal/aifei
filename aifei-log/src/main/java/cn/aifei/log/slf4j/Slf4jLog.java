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

package cn.aifei.log.slf4j;

import cn.aifei.log.Log;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;
import java.util.function.Supplier;

/**
 * 适配实现了 LocationAwareLogger 的 SLF4J 后端，如 Logback、Log4j 2、JUL。
 * 应在启动线程完成 SLF4J 初始化后，再启动使用日志的业务线程。
 */
public class Slf4jLog implements Log {

    private final LocationAwareLogger log;
    private static final String FQCN = Slf4jLog.class.getName();

    /**
     * 无参构造仅在 static 属性中使用，避免性能消耗
     */
    public Slf4jLog() {
        log = (LocationAwareLogger) LoggerFactory.getLogger(getCallerName());
    }

    public Slf4jLog(Class<?> clazz) {
        log = (LocationAwareLogger) LoggerFactory.getLogger(clazz);
    }

    public Slf4jLog(String name) {
        log = (LocationAwareLogger) LoggerFactory.getLogger(name);
    }

    private static String getCallerName() {
        StackTraceElement[] stack = new Throwable().getStackTrace();
        int start = 0;
        // 优先以 Log.get 为边界，避免将自定义日志工厂识别为业务调用者。
        for (int i = 0; i < stack.length; i++) {
            if (stack[i].getClassName().equals(Log.class.getName())) {
                start = i + 1;
                break;
            }
        }
        for (int i = start; i < stack.length; i++) {
            String name = stack[i].getClassName();
            if (!name.equals(FQCN) && !name.equals(Slf4jLogFactory.class.getName())
                    && !name.equals(Log.class.getName())
                    && !name.startsWith("java.lang.reflect.")
                    && !name.startsWith("jdk.internal.reflect.")
                    && !name.startsWith("sun.reflect.")
                    && !name.equals("java.lang.Class")
                    && !name.startsWith("java.lang.invoke.")) {
                return name;
            }
        }
        return FQCN;
    }

    @Override
    public String getName() {
        return log.getName();
    }

    // TRACE
    @Override
    public boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    @Override
    public void trace(String msg, Throwable t) {
        log.log(null, FQCN, LocationAwareLogger.TRACE_INT, msg, null, t);
    }

    @Override
    public void trace(String msg) {
        log.log(null, FQCN, LocationAwareLogger.TRACE_INT, msg, null, null);
    }

    @Override
    public void trace(String format, Object arg) {
        if (log.isTraceEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.TRACE_INT, format, new Object[]{arg}, null);
        }
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        if (log.isTraceEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.TRACE_INT, format, new Object[]{arg1, arg2}, null);
        }
    }

    @Override
    public void trace(String format, Object... arguments) {
        if (log.isTraceEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.TRACE_INT, format, arguments, null);
        }
    }

    @Override
    public void trace(Supplier<String> supplier) {
        if (log.isTraceEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.TRACE_INT, supplier.get(), null, null);
        }
    }

    // DEBUG
    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public void debug(String msg, Throwable t) {
        log.log(null, FQCN, LocationAwareLogger.DEBUG_INT, msg, null, t);
    }

    @Override
    public void debug(String msg) {
        log.log(null, FQCN, LocationAwareLogger.DEBUG_INT, msg, null, null);
    }

    @Override
    public void debug(String format, Object arg) {
        if (log.isDebugEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.DEBUG_INT, format, new Object[]{arg}, null);
        }
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (log.isDebugEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.DEBUG_INT, format, new Object[]{arg1, arg2}, null);
        }
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (log.isDebugEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.DEBUG_INT, format, arguments, null);
        }
    }

    @Override
    public void debug(Supplier<String> supplier) {
        if (log.isDebugEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.DEBUG_INT, supplier.get(), null, null);
        }
    }

    // INFO
    @Override
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    @Override
    public void info(String msg, Throwable t) {
        log.log(null, FQCN, LocationAwareLogger.INFO_INT, msg, null, t);
    }

    @Override
    public void info(String msg) {
        log.log(null, FQCN, LocationAwareLogger.INFO_INT, msg, null, null);
    }

    @Override
    public void info(String format, Object arg) {
        if (log.isInfoEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.INFO_INT, format, new Object[]{arg}, null);
        }
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if (log.isInfoEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.INFO_INT, format, new Object[]{arg1, arg2}, null);
        }
    }

    @Override
    public void info(String format, Object... arguments) {
        if (log.isInfoEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.INFO_INT, format, arguments, null);
        }
    }

    @Override
    public void info(Supplier<String> supplier) {
        if (log.isInfoEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.INFO_INT, supplier.get(), null, null);
        }
    }

    // WARN
    @Override
    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    @Override
    public void warn(String msg, Throwable t) {
        log.log(null, FQCN, LocationAwareLogger.WARN_INT, msg, null, t);
    }

    @Override
    public void warn(String msg) {
        log.log(null, FQCN, LocationAwareLogger.WARN_INT, msg, null, null);
    }

    @Override
    public void warn(String format, Object arg) {
        if (log.isWarnEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.WARN_INT, format, new Object[]{arg}, null);
        }
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        if (log.isWarnEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.WARN_INT, format, new Object[]{arg1, arg2}, null);
        }
    }

    @Override
    public void warn(String format, Object... arguments) {
        if (log.isWarnEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.WARN_INT, format, arguments, null);
        }
    }

    @Override
    public void warn(Supplier<String> supplier) {
        if (log.isWarnEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.WARN_INT, supplier.get(), null, null);
        }
    }

    // ERROR
    @Override
    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    @Override
    public void error(String msg, Throwable t) {
        log.log(null, FQCN, LocationAwareLogger.ERROR_INT, msg, null, t);
    }

    @Override
    public void error(String msg) {
        log.log(null, FQCN, LocationAwareLogger.ERROR_INT, msg, null, null);
    }

    @Override
    public void error(String format, Object arg) {
        if (log.isErrorEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.ERROR_INT, format, new Object[]{arg}, null);
        }
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        if (log.isErrorEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.ERROR_INT, format, new Object[]{arg1, arg2}, null);
        }
    }

    @Override
    public void error(String format, Object... arguments) {
        if (log.isErrorEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.ERROR_INT, format, arguments, null);
        }
    }

    @Override
    public void error(Supplier<String> supplier) {
        if (log.isErrorEnabled()) {
            log.log(null, FQCN, LocationAwareLogger.ERROR_INT, supplier.get(), null, null);
        }
    }
}


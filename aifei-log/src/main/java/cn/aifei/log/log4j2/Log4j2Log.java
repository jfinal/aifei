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

package cn.aifei.log.log4j2;

import cn.aifei.log.Log;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.ExtendedLogger;
import java.util.function.Supplier;

/**
 * Log4j2Log
 */
public class Log4j2Log implements Log {

    private final ExtendedLogger log;
    private static final String FQCN = Log4j2Log.class.getName();

    /**
     * 无参构造仅在 static 属性中使用，避免性能消耗
     */
    public Log4j2Log() {
        // Class<?> clazz = StackLocatorUtil.getCallerClass(4);
        // StackLocator API（Log4j 2.12+）动态计算深度：
        Class<?> clazz = org.apache.logging.log4j.util.StackLocator.getInstance().getCallerClass(Log.class);
        this.log = (ExtendedLogger) LogManager.getLogger(clazz != null ? clazz : Log4j2Log.class);
    }

    public Log4j2Log(Class<?> clazz) {
        // 引入 log4j-slf4j2-impl 桥接依赖后可能出现类型转换异常，可使用此方案
        // LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        // this.log = ctx.getLogger(clazz.getName());

        this.log = (ExtendedLogger) LogManager.getLogger(clazz);
    }

    public Log4j2Log(String name) {
        this.log = (ExtendedLogger) LogManager.getLogger(name);
    }

    @Override
    public String getName() {
        return log.getName();
    }

    // TRACE
    @Override
    public boolean isTraceEnabled() {
        return log.isEnabled(Level.TRACE);
    }

    @Override
    public void trace(String msg, Throwable t) {
        log.logIfEnabled(FQCN, Level.TRACE, null, msg, t);
    }

    @Override
    public void trace(String msg) {
        log.logIfEnabled(FQCN, Level.TRACE, null, msg);
    }

    @Override
    public void trace(String format, Object arg) {
        log.logIfEnabled(FQCN, Level.TRACE, null, format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        log.logIfEnabled(FQCN, Level.TRACE, null, format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        log.logIfEnabled(FQCN, Level.TRACE, null, format, arguments);
    }

    @Override
    public void trace(Supplier<String> messageSupplier) {
        log.logIfEnabled(FQCN, Level.TRACE, null, messageSupplier::get, null);
    }

    // DEBUG
    @Override
    public boolean isDebugEnabled() {
        return log.isEnabled(Level.DEBUG);
    }

    @Override
    public void debug(String msg, Throwable t) {
        log.logIfEnabled(FQCN, Level.DEBUG, null, msg, t);
    }

    @Override
    public void debug(String msg) {
        log.logIfEnabled(FQCN, Level.DEBUG, null, msg);
    }

    @Override
    public void debug(String format, Object arg) {
        log.logIfEnabled(FQCN, Level.DEBUG, null, format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        log.logIfEnabled(FQCN, Level.DEBUG, null, format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        log.logIfEnabled(FQCN, Level.DEBUG, null, format, arguments);
    }

    @Override
    public void debug(Supplier<String> messageSupplier) {
        log.logIfEnabled(FQCN, Level.DEBUG, null, messageSupplier::get, null);
    }

    // INFO
    @Override
    public boolean isInfoEnabled() {
        return log.isEnabled(Level.INFO);
    }

    @Override
    public void info(String msg, Throwable t) {
        log.logIfEnabled(FQCN, Level.INFO, null, msg, t);
    }

    @Override
    public void info(String msg) {
        log.logIfEnabled(FQCN, Level.INFO, null, msg);
    }

    @Override
    public void info(String format, Object arg) {
        log.logIfEnabled(FQCN, Level.INFO, null, format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        log.logIfEnabled(FQCN, Level.INFO, null, format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        log.logIfEnabled(FQCN, Level.INFO, null, format, arguments);
    }

    @Override
    public void info(Supplier<String> messageSupplier) {
        log.logIfEnabled(FQCN, Level.INFO, null, messageSupplier::get, null);
    }

    // WARN
    @Override
    public boolean isWarnEnabled() {
        return log.isEnabled(Level.WARN);
    }

    @Override
    public void warn(String msg, Throwable t) {
        log.logIfEnabled(FQCN, Level.WARN, null, msg, t);
    }

    @Override
    public void warn(String msg) {
        log.logIfEnabled(FQCN, Level.WARN, null, msg);
    }

    @Override
    public void warn(String format, Object arg) {
        log.logIfEnabled(FQCN, Level.WARN, null, format, arg);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        log.logIfEnabled(FQCN, Level.WARN, null, format, arg1, arg2);
    }

    @Override
    public void warn(String format, Object... arguments) {
        log.logIfEnabled(FQCN, Level.WARN, null, format, arguments);
    }

    @Override
    public void warn(Supplier<String> messageSupplier) {
        log.logIfEnabled(FQCN, Level.WARN, null, messageSupplier::get, null);
    }

    // ERROR
    @Override
    public boolean isErrorEnabled() {
        return log.isEnabled(Level.ERROR);
    }

    @Override
    public void error(String msg, Throwable t) {
        log.logIfEnabled(FQCN, Level.ERROR, null, msg, t);
    }

    @Override
    public void error(String msg) {
        log.logIfEnabled(FQCN, Level.ERROR, null, msg);
    }

    @Override
    public void error(String format, Object arg) {
        log.logIfEnabled(FQCN, Level.ERROR, null, format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        log.logIfEnabled(FQCN, Level.ERROR, null, format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        log.logIfEnabled(FQCN, Level.ERROR, null, format, arguments);
    }

    @Override
    public void error(Supplier<String> messageSupplier) {
        log.logIfEnabled(FQCN, Level.ERROR, null, messageSupplier::get, null);
    }
}



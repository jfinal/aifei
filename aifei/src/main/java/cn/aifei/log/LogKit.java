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

import java.util.Objects;

/**
 * LogKit.
 */
public class LogKit {

    static LogFactory logFactory = NoLogFactory.INSTANCE;

    private LogKit() {initDefaultLogFactory();}
    static LogKit kit = new LogKit();
    public static LogKit get() {return kit;}

    /**
     * 初始化默认日志工厂：
     * 1: 优先初始化 Slf4jLogFactory（pom.xml 引入 slf4j 依赖）
     * 2: 然后初始化 Log4j2LogFactory（pom.xml 引入 log4j2 依赖）
     * 3: 可通过 LogKit.get().setLogFactory(...) 覆盖默认日志工厂
     */
    private static void initDefaultLogFactory() {
        try {
            Class.forName("org.slf4j.Logger");
            logFactory = (LogFactory) Class.forName("cn.aifei.log.slf4j.Slf4jLogFactory").newInstance();
            logFactory.getLog(LogKit.class);            // 进一步探测 slf4j 实现是否存在
        } catch (Exception ignore) {
            try {
                Class.forName("org.apache.logging.log4j.Logger");
                logFactory = (LogFactory) Class.forName("cn.aifei.log.log4j2.Log4j2LogFactory").newInstance();
            } catch (Exception ignored) {
                logFactory = NoLogFactory.INSTANCE;
            }
        }
    }

    public void setLogFactory(LogFactory logFactory) {
        Objects.requireNonNull(logFactory, "logFactory can not be null.");
        LogKit.logFactory = logFactory;
    }

    public LogFactory getLogFactory() {
        return logFactory;
    }
}



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
import cn.aifei.log.LogFactory;

/**
 * Log4j2LogFactory
 */
public class Log4j2LogFactory implements LogFactory {

    @Override
    public Log getLog() {
        return new Log4j2Log();
    }

    @Override
    public Log getLog(Class<?> clazz) {
        return new Log4j2Log(clazz);
    }

    @Override
    public Log getLog(String name) {
        return new Log4j2Log(name);
    }
}

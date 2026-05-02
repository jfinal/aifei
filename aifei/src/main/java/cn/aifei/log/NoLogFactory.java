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

/**
 * NoLogFactory 用于提示使用者配置 LogFactory，日志必须可用。
 * <p>
 * 注意：NoLogFactory 不同于 NullLogFactory，后者是 Null Object Pattern，
 *      只解决 null 值判断问题，不抛出异常。
 */
public class NoLogFactory implements LogFactory {

    public static final NoLogFactory INSTANCE = new NoLogFactory();

    private NoLogFactory() {}

    @Override
    public Log getLog() {
        throw new IllegalStateException("LogFactory not configured.");
    }

    @Override
    public Log getLog(Class<?> clazz) {
        throw new IllegalStateException("LogFactory not configured.");
    }

    @Override
    public Log getLog(String name) {
        throw new IllegalStateException("LogFactory not configured.");
    }
}



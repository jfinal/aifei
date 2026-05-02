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

import cn.aifei.core.Input;
import cn.aifei.core.Output;

/**
 * AifeiConfig.
 */
public interface AifeiConfig<I extends Input, O extends Output> {

    /**
     * 偏好配置
     */
    void config(Settings<I, O> settings);

    /**
     * 路由配置
     */
    void config(Routes routes);

    /**
     * 插件配置
     */
    void config(Plugins plugins);

    /**
     * 系统启动时回调（注：config 系列方法调用之后以及所有 Plugin 启动之后回调，回调时可使用 Plugin 功能）
     */
    default void onStart() {}

    /**
     * 系统关闭时回调（注：服务器关闭之后 Plugin 关闭之前回调，回调时仍然可以使用 Plugin 功能）
     */
    default void onStop() {}
}


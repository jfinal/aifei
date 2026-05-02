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

package cn.aifei.db.core;

import cn.aifei.plugin.Plugin;
import javax.sql.DataSource;
import java.util.function.Supplier;

/**
 * AifeiDbPlugin 实现 aifei 内核 Plugin 接口，便于在 AifeiConfig.config(Plugins plugins)
 * 中整合 aifei-db 项目使用。
 */
public class AifeiDbPlugin extends AifeiDb implements Plugin {

    /**
     * AifeiDb 构造
     *
     * @param configId   配置 id
     * @param dataSource 数据源
     */
    public AifeiDbPlugin(String configId, DataSource dataSource) {
        super(configId, dataSource);
    }

    /**
     * AifeiDb 构造
     *
     * @param configId   配置 id
     * @param dataSourceSupplier 数据源提供函数
     */
    public AifeiDbPlugin(String configId, Supplier<DataSource> dataSourceSupplier) {
        super(configId, dataSourceSupplier);
    }

    /**
     * 支持无数据库场景，例如仅将 model 当成 Java Bean 使用的场景。
     * 例：new AifeiDb().addModelSet(...).start();
     */
    public AifeiDbPlugin() {

    }
}


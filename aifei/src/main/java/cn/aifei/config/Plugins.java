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

import cn.aifei.plugin.Plugin;
import java.util.ArrayList;
import java.util.List;

/**
 * Plugins
 */
public class Plugins {

    private final List<Plugin> pluginList = new ArrayList<>();

    public Plugins add(Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin can not be null.");
        }
        pluginList.add(plugin);
        return this;
    }

    public List<Plugin> getPluginList() {
        return pluginList;
    }
}




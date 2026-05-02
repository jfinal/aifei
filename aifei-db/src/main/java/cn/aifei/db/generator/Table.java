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

package cn.aifei.db.generator;

import cn.aifei.db.core.AifeiRow;
import java.util.Collections;
import java.util.Map;

/**
 * Table 用于封装数据库 table 的 meta 信息
 */
public class Table {

    public final String name;
    public final String fields;
    public final String[] primaryKey;
    public final Class<? extends AifeiRow<?>> modelType;

    // fieldTypeMap 不仅映射了字段类型，还实现了 columnDefined 功能
    public final Map<String, Class<?>> fieldTypeMap;

    public Table(String name, String fields, String[] primaryKey, Class<? extends AifeiRow<?>> modelType, Map<String, Class<?>> fieldTypeMap) {
        this.name = name;
        this.fields = fields;
        this.primaryKey = primaryKey;
        this.modelType = modelType;
        this.fieldTypeMap = Collections.unmodifiableMap(fieldTypeMap);
    }
}

/*
 * 尝试过的备选方案:
 * public interface Table {
 *     String name = "user";
 *     String FIELDS = "id,name,age,created,updated";
 *     String[] PRIMARY_KEY = new String[]{"id"};
 *     Map<String, Class<?>> fieldTypeMap = Collections.unmodifiableMap(new HashMap<String, Class<?>>() {{
 *         put("id", Integer.class);
 *         put("name", String.class);
 *         put("age", Integer.class);
 *         put("created", java.util.Date.class);
 *         put("updated", java.util.Date.class);
 *     }});
 * }
 */

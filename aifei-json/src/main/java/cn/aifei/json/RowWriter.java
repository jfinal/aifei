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

package cn.aifei.json;

import cn.aifei.db.core.Row;
import cn.aifei.util.ComputeCache;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * RowWriter 用于实现 aifei-db 的 Row 对象转换为 json 字符串。
 *
 * <pre>
 * Row 对象 转 json 字符串逻辑：
 *  1: Row.data() 取出内部数据 Map data，然后对该 Map 对象进行转换。
 *  2: 若 snakeToCamel 为 true，则将字段由下划线转为驼峰风格，对前端开发友好。
 *  3: 若 lowerBeforeCamel 为 true，则在下划线转为驼峰时将字段字符转成小写字母，对 Oracle 等数据库友好。
 * </pre>
 */
public class RowWriter implements ObjectWriter<Row> {

    static final ComputeCache<String, String> KEY_CACHE = new ComputeCache<>(1024);

    @Override
    public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
        if (object == null) {
            jsonWriter.writeNull();
            return;
        }

        Row row = (Row) object;
        Map<String, Object> data = row.data();
        if (data == null || data.isEmpty()) {
            jsonWriter.startObject();
            jsonWriter.endObject();
            return;
        }

        jsonWriter.startObject();
        JsonObject jsonObject = Cpc.getThreadLocalJsonObject();
        if (jsonObject != null && Cpc.getSnakeToCamel(jsonObject)) {            // null 判断避免 JSON.toJSON(...) 抛出 NPE
            boolean lowerBeforeCamel = Cpc.getLowerBeforeCamel(jsonObject);
            for (Map.Entry<String, Object> e : data.entrySet()) {
                String key = lowerBeforeCamel ? e.getKey() + '1' : e.getKey();  // 缓存的 key 需 lowerBeforeCamel 参与
                String camelName = KEY_CACHE.computeIfAbsent(key, doNotUse -> snakeToCamel(e.getKey(), lowerBeforeCamel));
                jsonWriter.writeNameValue(camelName, e.getValue());
            }
        } else {
            for (Map.Entry<String, Object> e : data.entrySet()) {
                jsonWriter.writeNameValue(e.getKey(), e.getValue());
            }
        }
        jsonWriter.endObject();
    }

    // 下划线转驼峰风格
    String snakeToCamel(String fieldName, boolean lowerCase) {
        if (fieldName == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean toUpperCase = false;

        for (int i = 0, len = fieldName.length(); i < len; i++) {
            char c = fieldName.charAt(i);
            if (c == '_') {
                toUpperCase = true;
            } else {
                if (toUpperCase) {
                    sb.append(Character.toUpperCase(c));
                    toUpperCase = false;
                } else {
                    if (lowerCase) {
                        // 支持字段名为大写的数据库，如 Oracle
                        sb.append(Character.toLowerCase(c));
                    } else {
                        // 支持字段名为小写的数据库，如 Mysql
                        sb.append(c);
                    }
                }
            }
        }

        return sb.toString();
    }
}



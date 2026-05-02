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
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * RowReader 用于实现 json 字符串转换为 aifei-db 的 Row 对象。
 *
 * <pre>
 * json 字符串 转 Row 对象逻辑：
 *  1: 创建 Row 对象。
 *  2: 通过 fastjson API 读取 json 的 key、value 并调用 Row.set(key, value) 将其存入 Row 对象。
 *  3: 若 camelToSnake 为 true，则将字段由驼峰转为下划线风格，对接数据库字段名，对数据库开发友好。
 * </pre>
 */
public class RowReader implements ObjectReader<Row> {

    static final ComputeCache<String, String> KEY_CACHE = new ComputeCache<>(1024);

    @Override
    public Row readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        if (jsonReader.nextIfNull()) {
            return null;
        }

        jsonReader.nextIfObjectStart();
        Row row = new Row();
        JsonString jsonString = Cpc.getThreadLocalJsonString();
        if (jsonString != null && Cpc.getCamelToSnake(jsonString)) {    // null 判断避免 JSON.parse(...) 抛出 NPE
            while (!jsonReader.nextIfObjectEnd()) {
                String name = jsonReader.readFieldName();
                String snakeName = KEY_CACHE.computeIfAbsent(name, this::camelToSnake);
                Object value = jsonReader.readAny();
                row.setOrPut(snakeName, value);         // 未使用 set(...)，只允许存放正确字段使用 modelAsRow = false 参数
            }
        } else {
            while (!jsonReader.nextIfObjectEnd()) {
                String name = jsonReader.readFieldName();
                // 读任意值：基础类型、List、Map、嵌套对象等，fastjson2 会返回合适的 Java 类型
                Object value = jsonReader.readAny();
                row.setOrPut(name, value);              // 未使用 set(...)，只允许存放正确字段使用 modelAsRow = false 参数
            }
        }

        return row;
    }

    /*
     * 支持 JSONObject.to(...) 与 JSONObject.getObject(...)
     */
    @Override
    public Row createInstance(Map map, long features) {
        if (map == null) {
            return null;
        }

        Row row = new Row();
        JsonString jsonString = Cpc.getThreadLocalJsonString();
        if (jsonString != null && Cpc.getCamelToSnake(jsonString)) {    // TODO 该分支未能进入，后续测试完善
            map.forEach((k, v) -> {
                String name = k.toString();
                String snakeName = KEY_CACHE.computeIfAbsent(name, this::camelToSnake);
                row.setOrPut(snakeName, v);
            });

        } else {
            map.forEach((k, v) -> row.setOrPut(k.toString(), v));
        }

        return row;
    }

    // 驼峰转下划线风格
    String camelToSnake(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = fieldName.length(); i < len; i++) {
            char c = fieldName.charAt(i);
            // 如果是大写字母，则前面补下划线并转小写
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}



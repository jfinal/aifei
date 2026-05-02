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

import com.alibaba.fastjson2.JSON;
import java.io.OutputStream;
import java.util.function.BiFunction;

/**
 * JsonObject 将 Java 对象转换为 json 字符串。
 */
public class JsonObject {

    static final ThreadLocal<JsonObject> JSON_OBJECT_LOCAL = new ThreadLocal<>();

    static boolean defaultSnakeToCamel = true;
    static boolean defaultLowerBeforeCamel = false;
    static boolean defaultModelAsRow = false;

    Boolean snakeToCamel;       // 下划线转驼峰
    Boolean lowerBeforeCamel;   // 下划线转驼峰时是否先将其转成小写，适用于 oracle 数据库
    Boolean modelAsRow;         // Model 当成 Row 去转换 json，而非通过 getter 方法取值转换
    String dateFormat;          // 日期格式化

    final Object object;

    public JsonObject(Object object) {
        this.object = object;
    }

    /**
     * Java 对象转换为 json 字符串
     */
    public String toJson() {
        try {
            JSON_OBJECT_LOCAL.set(this);
            if (dateFormat != null) {
                return JSON.toJSONString(object, dateFormat);
            } else {
                return JSON.toJSONString(object);
            }
        } finally {
            JSON_OBJECT_LOCAL.remove();
        }
    }

    /**
     * Java 对象转换为 json 字符串并直接输出到 OutputStream
     */
    public void toJson(OutputStream outputStream) {
        try {
            JSON_OBJECT_LOCAL.set(this);
            if (dateFormat != null) {
                JSON.writeTo(outputStream, object, dateFormat, null);
            } else {
                JSON.writeTo(outputStream, object);
            }
        } finally {
            JSON_OBJECT_LOCAL.remove();
        }
    }

    /**
     * 通过函数充分利用 fastjson API 定制转换逻辑，并可在 RowWriter、ModelWriter 中用上存放在
     * ThreadLocal 内的 JsonObject.snakeToCamel/modelAsRow 等等参数。
     *
     * <pre>
     * 例子：
     *  1: 在函数中使用 fastjson 的 Feature.PrettyFormat 格式化 json
     *     User user = new User().name("James");
     *     String json = Json.of(user).toJson((object, dateFormat) -> {
     *         if (dateFormat != null) {
     *             return JSON.toJSONString(object, dateFormat, JSONWriter.Feature.PrettyFormat);
     *         } else {
     *             return JSON.toJSONString(object, JSONWriter.Feature.PrettyFormat);
     *         }
     *     });
     *
     *  2: 在函数中将输出指向 OutputStream 的同时，可充分利用 fastjson 的 JSONWriter.Feature 机制
     *     Json.of(user).toJson((object, dateFormat) -> {
     *         JSON.writeTo(System.out, object, JSONWriter.Feature.PrettyFormat);
     *         return null;
     *     });
     * </pre>
     */
    public String toJson(BiFunction<Object, String, String> fun) {
        try {
            JSON_OBJECT_LOCAL.set(this);
            return fun.apply(object, dateFormat);
        } finally {
            JSON_OBJECT_LOCAL.remove();
        }
    }

    public String toString() {
        // return toJson(); // 坑：触发 JSON_OBJECT_LOCAL.remove()，调试时抛出 NPE
        return String.valueOf(object);
    }

    /**
     * 设置 snakeToCamel 为 true。
     * 注意：仅针对 Row 对象。
     */
    public JsonObject snakeToCamel() {
        snakeToCamel = true;
        return this;
    }

    /**
     * 设置 snakeToCamel 为 true，以及 lowerBeforeCamel 为 true
     * 注意：仅针对 Row 对象。
     */
    public JsonObject snakeToCamelLower() {
        snakeToCamel = true;
        lowerBeforeCamel = true;
        return this;
    }

    /**
     * 设置 snakeToCamel 为 true，以及 lowerBeforeCamel 为 false
     * 注意：仅针对 Row 对象。
     */
    public JsonObject snakeToCamelKeep() {
        snakeToCamel = true;
        lowerBeforeCamel = false;
        return this;
    }

    /**
     * 设置 snakeToCamel。仅针对 aifei-db 内的 Row 以及 Model。
     * 注意：仅针对 Row 对象。
     */
    public JsonObject snakeToCamel(boolean enabled) {
        snakeToCamel = enabled;
        return this;
    }

    /**
     * 设置 lowerBeforeCamel 为 true。当 snakeToCamel 为 true 将字段由下划线转驼峰之前先将字段转换为小写。
     * 注意：仅针对 Row 对象。
     */
    public JsonObject lowerBeforeCamel() {
        lowerBeforeCamel = true;
        return this;
    }

    /**
     * 设置 lowerBeforeCamel。当 snakeToCamel 为 true 将字段由下划线转驼峰之前，是否先将字段转换为小写。
     * 注意：仅针对 Row 对象。
     */
    public JsonObject lowerBeforeCamel(boolean enabled) {
        lowerBeforeCamel = enabled;
        return this;
    }

    /**
     * 设置 modelAsRow 为 true，即将 aifei-db 中的 Model 当成 Row 转换，而非调用 getter 取值转换。
     * 用于 sql 关联查询返回 Model 对象，并且需要将关联表字段转换为 json 的场景。
     */
    public JsonObject modelAsRow() {
        modelAsRow = true;
        return this;
    }

    /**
     * 设置 modelAsRow，即将 aifei-db 中的 Model 当成 Row 转换，而非调用 getter 取值转换。
     * 用于 sql 关联查询返回 Model 对象，并且需要将关联表字段转换为 json 的场景。
     */
    public JsonObject modelAsRow(boolean enabled) {
        modelAsRow = enabled;
        return this;
    }

    /**
     * 设置日期格式
     */
    public JsonObject dateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
        return this;
    }
}



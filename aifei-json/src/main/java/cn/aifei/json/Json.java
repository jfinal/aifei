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

/**
 * Json API 入口。
 */
public class Json {

    /**
     * 创建 JsonString，用于将 json 字符串转换为 Java 对象
     */
    public static JsonString of(String str) {
        return JsonKit.jsonFactory.getJsonString(str);
    }

    /**
     * 创建 JsonObject，用于将 Java 对象转换为 json 字符串
     */
    public static JsonObject of(Object object) {
        if (object instanceof String) {
            throw new IllegalArgumentException("Use Json.of(String) instead of Json.of(Object) for json string input.");
        }
        return JsonKit.jsonFactory.getJsonObject(object);
    }
}



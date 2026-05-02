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
 * 跨包调用 Cpc（Cross package call）。
 */
public class Cpc {

    public static JsonString getThreadLocalJsonString() {
        return JsonString.JSON_STRING_LOCAL.get();
    }

    public static JsonObject getThreadLocalJsonObject() {
        return JsonObject.JSON_OBJECT_LOCAL.get();
    }

    public static boolean getCamelToSnake(JsonString jsonString) {
        return jsonString.camelToSnake != null ? jsonString.camelToSnake : JsonString.defaultCamelToSnake;
    }

    public static boolean getModelAsRow(JsonString jsonString) {
        return jsonString.modelAsRow != null ? jsonString.modelAsRow : JsonString.defaultModelAsRow;
    }

    public static boolean getSnakeToCamel(JsonObject jsonObject) {
        return jsonObject.snakeToCamel != null ? jsonObject.snakeToCamel : JsonObject.defaultSnakeToCamel;
    }

    public static boolean getLowerBeforeCamel(JsonObject jsonObject) {
        return jsonObject.lowerBeforeCamel != null ? jsonObject.lowerBeforeCamel : JsonObject.defaultLowerBeforeCamel;
    }

    public static boolean getModelAsRow(JsonObject jsonObject) {
        return jsonObject.modelAsRow != null ? jsonObject.modelAsRow : JsonObject.defaultModelAsRow;
    }

    // 用于扩展功能，或者结合 JsonFactory 切换 JsonString 实现
    public static void setThreadLocalJsonString(JsonString jsonString) {
        JsonString.JSON_STRING_LOCAL.set(jsonString);
    }

    // 用于扩展功能，或者结合 JsonFactory 切换 JsonObject 实现
    public static void setThreadLocalJsonObject(JsonObject jsonObject) {
        JsonObject.JSON_OBJECT_LOCAL.set(jsonObject);
    }
}



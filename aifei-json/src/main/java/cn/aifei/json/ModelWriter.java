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

import cn.aifei.db.core.AifeiRow;
import cn.aifei.db.core.Row;
import cn.aifei.util.ComputeCache;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterCreator;
import java.lang.reflect.Type;

/**
 * ModelWriter 用于实现 aifei-db 的 Model 转换为 json 字符串。
 *
 * <pre>
 * Model 转换为 json 逻辑：
 *  1: 若 modelAsRow 为 true，则按 Row 对象的转换逻辑转换。
 *  2: 若 modelAsRow 为 false，则按 java bean 通过 getter 取值并转换。
 * </pre>
 */
public class ModelWriter implements ObjectWriter<Object> {

    static final ComputeCache<Class<?>, ObjectWriter<Object>> CACHE = new ComputeCache<>(512);

    @Override @SuppressWarnings({"unchecked"})
    public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
        if (object == null) {
            jsonWriter.writeNull();
            return;
        }

        // modelAsRow 为 true 使用 Row 转换规则
        JsonObject jsonObject = Cpc.getThreadLocalJsonObject();
        if (jsonObject != null && Cpc.getModelAsRow(jsonObject)) {  // null 判断避免 JSON.toJSON(...) 抛出 NPE
            Row row = new Row().data(((AifeiRow<?>) object).data());
            jsonWriter.writeAs(row, Row.class);
            return;
        }

        // 使用 ObjectWriterCreator 直接为具体 Class 生成默认 writer，不查 ObjectWriterProvider 的注册表，
        // 避免回调到本 ModelWriter 造成无限递归
        ObjectWriter<Object> writer = CACHE.computeIfAbsent(object.getClass(),
                t -> (ObjectWriter<Object>) ObjectWriterCreator.INSTANCE.createObjectWriter(t)
        );
        writer.write(jsonWriter, object, fieldName, fieldType, features);
    }
}




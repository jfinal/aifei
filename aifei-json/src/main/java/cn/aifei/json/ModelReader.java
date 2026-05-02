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
import cn.aifei.enjoy.util.InstanceUtil;
import cn.aifei.util.ComputeCache;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderCreator;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * ModelReader 用于实现 json 字符串转换为 aifei-db 的 Model。
 *
 * <pre>
 * json 字符串转换 Model 逻辑：
 *  1: 若 modelAsRow 为 true，则先按 Row 类型转换，然后将 Row 中的数据赋值到 Model 之中。
 *  2: 若 modelAsRow 为 false，则按 java bean 通过 setter 方法转换。
 * </pre>
 */
public class ModelReader implements ObjectReader<Object> {

    static final ComputeCache<Class<?>, ObjectReader<Object>> CACHE = new ComputeCache<>(512);

    final Class<?> type;

    public ModelReader(Class<?> type) {
        this.type = type;
    }

    @Override @SuppressWarnings("unchecked")
    public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        if (jsonReader.nextIfNull()) {
            return null;
        }

        // modelAsRow 为 true 使用 Row 转换规则，然后将 row 的数据存入 Model 并返回
        JsonString jsonString = Cpc.getThreadLocalJsonString();
        if (jsonString != null && Cpc.getModelAsRow(jsonString)) {  // null 判断避免 JSON.parse(...) 抛出 NPE
            Row row = jsonReader.read(Row.class);
            if (row != null) {
                AifeiRow<?> model = (AifeiRow<?>) InstanceUtil.get(type);
                return model.data(row);
            } else {
                return null;
            }
        }

        // modelAsRow 为 false 按 java bean 通过 setter 方法转换
        ObjectReader<Object> reader = CACHE.computeIfAbsent(type,
                t -> (ObjectReader<Object>) ObjectReaderCreator.INSTANCE.createObjectReader(t)
        );
        return reader.readObject(jsonReader, fieldType, fieldName, features);
    }

    static final ComputeCache<Class<?>, ObjectReader<Object>> FASTJSON_READER_CACHE = new ComputeCache<>(512);

    /*
     * 支持 JSONObject.to(...) 与 JSONObject.getObject(...)
     * 注意：不能基于 createInstance(Map map, JSONReader.Feature... features)，因为只支持 JSONObject.getObject(...)
     *      不支持 JSONObject.to(...)
     *
     * 参考 ObjectReaderProvider.getObjectReaderInternal() 方法内的实现方式
     *       ObjectReaderCreator creator = getCreator();
     *       objectReader = creator.createObjectReader(objectClass, objectType, fieldBased, this);
     */
    @Override @SuppressWarnings("unchecked")
    public Object createInstance(Map map, long features) {
        ObjectReader<Object> reader = FASTJSON_READER_CACHE.computeIfAbsent(type, t -> {
            ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
            return (ObjectReader<Object>) provider.getCreator().createObjectReader(t, t, false, provider);
        });
        return reader.createInstance(map, features);
    }
}



// 先转成 json String 再调用 JSONReader.readObject(...) 支持 JSONObject.to(...) 与
// JSONObject.getObject(...)
// public Object createInstance(Map map, long features) {
//     if (!(map instanceof JSONObject)) {
//         throw new IllegalArgumentException("Json data must be JSONObject");
//     }
//
//     JSONObject obj = (JSONObject) map;
//     try (JSONReader jsonReader = JSONReader.of(obj.toJSONString(), JSONFactory.createReadContext(features))) {
//         ObjectReader<Object> reader = CACHE.computeIfAbsent(type,
//                 t -> (ObjectReader<Object>) ObjectReaderCreator.INSTANCE.createObjectReader(t)
//         );
//         return reader.readObject(jsonReader, null, null, getFeatures());
//     }
// }




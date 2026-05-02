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
import com.alibaba.fastjson2.modules.ObjectReaderModule;
import com.alibaba.fastjson2.reader.ObjectReader;
import java.lang.reflect.Type;

/**
 * ModelReaderModule 用于批量注册被转换类型到 ModelReader 的映射。
 * 注意：JSON.register(Class, ObjectReader) 一次只能注册一个映射。
 */
public class ModelReaderModule implements ObjectReaderModule {

    @Override
    public ObjectReader<?> getObjectReader(Type type) {
        // 以下两行可省去 RowReader 注册：JSON.register(Row.class, new RowReader())，但独立注册性能更高
        // if (type == Row.class) {
        //     return new RowReader();
        // }

        if (type instanceof Class && AifeiRow.class.isAssignableFrom((Class<?>) type)) {
            return new ModelReader((Class<?>) type);
        }

        return null;    // 交给默认机制
    }
}


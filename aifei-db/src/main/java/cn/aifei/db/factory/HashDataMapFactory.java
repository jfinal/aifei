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

package cn.aifei.db.factory;

import java.util.HashMap;
import java.util.Map;

/**
 * HashDataMapFactory 返回 HashMap 对象存放查询数据，在性能和内存占用上略优于 DataMapFactory，
 * 可用于数据量大但不关心 select 字段顺序的场景
 */
public class HashDataMapFactory extends DataMapFactory {

    public static final HashDataMapFactory instance = new HashDataMapFactory();

    @Override
    public Map<String, Object> get() {
        return new HashMap<>();
    }
}



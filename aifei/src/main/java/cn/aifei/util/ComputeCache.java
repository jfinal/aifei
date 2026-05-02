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

package cn.aifei.util;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * ComputeCache
 *
 * <pre>
 * 场景要求：
 *  1: 只存不删，key 数量必须有限。
 *  2: key 与 value 不为 null。
 *
 * 创建原因：
 *  1: 多处刚需，场景明确。
 *  2: 简化功能，缩短类名。
 *  3: JDK 部分版本 ConcurrentHashMap.computeIfAbsent(...) 存在 bug。
 * </pre>
 */
public final class ComputeCache<K, V> {

    static final int DEFAULT_INITIAL_CAPACITY = 50;
    static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    private final Object[] mutexes;
    private final ConcurrentHashMap<K, V> map;

    public ComputeCache() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_CONCURRENCY_LEVEL);
    }

    public ComputeCache(int initialCapacity) {
        this(initialCapacity, DEFAULT_CONCURRENCY_LEVEL);
    }

    public ComputeCache(int initialCapacity, int concurrencyLevel) {
        this.map = new ConcurrentHashMap<>(initialCapacity);

        int size = concurrencyLevel > 0 ? concurrencyLevel : DEFAULT_CONCURRENCY_LEVEL;
        this.mutexes = new Object[size];
        for (int i = 0; i < mutexes.length; i++) {
            mutexes[i] = new Object();
        }
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> fun) {
        V value = map.get(key);
        if (value != null) {
            return value;
        }

        // 负值 hashCode 需转为正值
        int index = (key.hashCode() & 0x7FFFFFFF) % mutexes.length;
        synchronized (mutexes[index]) {
            value = map.get(key);
            if (value != null) {
                return value;
            }

            value = fun.apply(key);
            Objects.requireNonNull(value, "Computed value can not be null.");
            map.put(key, value);
            return value;
        }
    }

    public V get(K key) {
        return map.get(key);
    }

    // public V put(K key, V value) {
    //     return map.put(key, value);
    // }

    // public V putIfAbsent(K key, V value) {
    //     return map.putIfAbsent(key, value);
    // }

    // public V remove(K key) {
    //     return map.remove(key);
    // }

    // public void clear() {
    //     map.clear();
    // }
}



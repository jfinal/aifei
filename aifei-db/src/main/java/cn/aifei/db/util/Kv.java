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

package cn.aifei.db.util;

import cn.aifei.db.core.TypeConverter;
import cn.aifei.enjoy.util.StrUtil;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Kv (Key Value)
 *
 * <pre>
 * 例子：
 *    Kv data = Kv.of("username", "aifei");
 *    User.sql("select * from user where username = #para(username)", data).find();
 * </pre>
 */
public class Kv extends LinkedHashMap<Object, Object> {

    private static final long serialVersionUID = -808251639784763326L;

    static TypeConverter typeConverter = new TypeConverter();

    public Kv() {
    }

    public static Kv of(Object key, Object value) {
        return new Kv().set(key, value);
    }

    public static Kv of() {
        return new Kv();
    }

    public Kv set(Object key, Object value) {
        super.put(key, value);
        return this;
    }

    public Kv unset(Object key) {
        super.remove(key);
        return this;
    }

    public Kv setIfNotBlank(Object key, String value) {
        if (StrUtil.notBlank(value)) {
            set(key, value);
        }
        return this;
    }

    public Kv setIfNotNull(Object key, Object value) {
        if (value != null) {
            set(key, value);
        }
        return this;
    }

    public Kv set(Map<?, ?> map) {
        super.putAll(map);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAs(Object key) {
        return (T) get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAs(Object key, T defaultValue) {
        Object ret = get(key);
        return ret != null ? (T) ret : defaultValue;
    }

    public <T> T getAs(Object key, Function<Object, T> converter) {
        Object ret = get(key);
        return ret != null ? converter.apply(ret) : null;
    }

    public <T> T getAs(Object key, T defaultValue, Function<Object, T> converter) {
        Object ret = get(key);
        return ret != null ? converter.apply(ret) : defaultValue;
    }

    public String getStr(Object key) {
        Object s = get(key);
        return s != null ? s.toString() : null;
    }

    public Integer getInt(Object key) {
        return typeConverter.toInt(get(key));
    }

    public Long getLong(Object key) {
        return typeConverter.toLong(get(key));
    }

    public BigDecimal getBigDecimal(Object key) {
        return typeConverter.toBigDecimal(get(key));
    }

    public Double getDouble(Object key) {
        return typeConverter.toDouble(get(key));
    }

    public Float getFloat(Object key) {
        return typeConverter.toFloat(get(key));
    }

    public Number getNumber(Object key) {
        return typeConverter.toNumber(get(key));
    }

    public Boolean getBoolean(Object key) {
        return typeConverter.toBoolean(get(key));
    }

    public Date getDate(Object key) {
        return typeConverter.toDate(get(key));
    }

    public LocalDateTime getLocalDateTime(Object key) {
        return typeConverter.toLocalDateTime(get(key));
    }

    public String getStr(Object key, String defaultValue) {
        Object s = get(key);
        return s != null ? s.toString() : defaultValue;
    }

    public Integer getInt(Object key, Integer defaultValue) {
        Object value = get(key);
        return value != null ? typeConverter.toInt(value) : defaultValue;
    }

    public Long getLong(Object key, Long defaultValue) {
        Object value = get(key);
        return value != null ? typeConverter.toLong(value) : defaultValue;
    }

    public BigDecimal getBigDecimal(Object key, BigDecimal defaultValue) {
        Object value = get(key);
        return value != null ? typeConverter.toBigDecimal(value) : defaultValue;
    }

    public Double getDouble(Object key, Double defaultValue) {
        Object value = get(key);
        return value != null ? typeConverter.toDouble(value) : defaultValue;
    }

    public Float getFloat(Object key, Float defaultValue) {
        Object value = get(key);
        return value != null ? typeConverter.toFloat(value) : defaultValue;
    }

    public Number getNumber(Object key, Number defaultValue) {
        Object value = get(key);
        return value != null ? typeConverter.toNumber(value) : defaultValue;
    }

    public Boolean getBoolean(Object key, Boolean defaultValue) {
        Object value = get(key);
        return value != null ? typeConverter.toBoolean(value) : defaultValue;
    }

    public Date getDate(Object key, Date defaultValue) {
        Object value = get(key);
        return value != null ? typeConverter.toDate(value) : defaultValue;
    }

    public LocalDateTime getLocalDateTime(Object key, LocalDateTime defaultValue) {
        Object value = get(key);
        return value != null ? typeConverter.toLocalDateTime(value) : defaultValue;
    }

    /**
     * key 存在，并且 value 不为 null
     */
    public boolean notNull(Object key) {
        return get(key) != null;
    }

    /**
     * key 不存在，或者 key 存在但 value 为null
     */
    public boolean isNull(Object key) {
        return get(key) == null;
    }

    /**
     * key 所对应的 value 值不为空白字符串
     */
    public boolean notBlank(Object key) {
        return StrUtil.notBlank(getStr(key));
    }

    /**
     * key 所对应的 value 值为空白字符串
     */
    public boolean isBlank(Object key) {
        return StrUtil.isBlank(getStr(key));
    }

    /**
     * key 存在，并且 value 为 true，则返回 true
     */
    public boolean isTrue(Object key) {
        Object value = get(key);
        return value != null && typeConverter.toBoolean(value);
    }

    /**
     * key 存在，并且 value 为 false，则返回 true
     */
    public boolean isFalse(Object key) {
        Object value = get(key);
        return value != null && !typeConverter.toBoolean(value);
    }

    public Kv keep(String... keys) {
        if (keys == null || keys.length == 0) {
            clear();
            return this;
        }

        Kv newKv = new Kv();
        for (String k : keys) {
            if (containsKey(k)) {	// 避免将并不存在的变量存为 null
                newKv.put(k, get(k));
            }
        }
        clear();
        putAll(newKv);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> castMap() {
        return (Map<K, V>) this;
    }

    // 使用父类 equals，以免破坏对称性
    // public boolean equals(Object kv) {
    //     return kv instanceof Kv && super.equals(kv);
    // }

    // 此处依赖 aifei-json 形成循环依赖
    // public String toJson() {
    //     return cn.aifei.json.Json.of(this).toJson();
    // }
}




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

package cn.aifei.db.core;

import cn.aifei.enjoy.util.InstanceUtil;
import cn.aifei.enjoy.util.StrUtil;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

/**
 * AifeiRow.
 */
@SuppressWarnings("unchecked")
public class AifeiRow<R extends AifeiRow<R>> implements Iterable<Map.Entry<String, Object>>, Serializable {

    protected String table;
    protected String[] primaryKey;
    protected Map<String, Object> data;
    protected Set<String> change;

    static TypeConverter typeConverter = new TypeConverter();

    public R table(String table) {
        if (StrUtil.isBlank(table)) {
            throw new IllegalArgumentException("table can not be blank.");
        }
        this.table = table.trim();
        return (R) this;
    }

    public R table(String table, String primaryKey) {
        if (StrUtil.isBlank(table)) {
            throw new IllegalArgumentException("table can not be blank.");
        }
        if (StrUtil.isBlank(primaryKey)) {
            throw new IllegalArgumentException("primaryKey can not be blank.");
        }
        this.table = table.trim();
        this.primaryKey = new String[]{primaryKey.trim()};
        return (R) this;
    }

    public R table(String table, String primaryKey1, String primaryKey2) {
        if (StrUtil.isBlank(table)) {
            throw new IllegalArgumentException("table can not be blank.");
        }
        if (StrUtil.isBlank(primaryKey1)) {
            throw new IllegalArgumentException("primaryKey1 can not be blank.");
        }
        if (StrUtil.isBlank(primaryKey2)) {
            throw new IllegalArgumentException("primaryKey2 can not be blank.");
        }
        this.table = table.trim();
        this.primaryKey = new String[]{primaryKey1.trim(), primaryKey2.trim()};
        return (R) this;
    }

    public String table() {
        return table;
    }

    public R primaryKey(String primaryKey) {
        if (StrUtil.isBlank(primaryKey)) {
            throw new IllegalArgumentException("primaryKey can not be blank.");
        }
        this.primaryKey = new String[]{primaryKey.trim()};
        return (R) this;
    }

    // 设置复合主键
    public R primaryKey(String... primaryKeys) {
        if (primaryKeys == null || primaryKeys.length == 0) {
            throw new IllegalArgumentException("primaryKeys can not be null.");
        }
        for (int i = 0; i < primaryKeys.length; i++) {
            if (StrUtil.isBlank(primaryKeys[i])) {
                throw new IllegalArgumentException("primaryKey can not be blank.");
            }
            primaryKeys[i] = primaryKeys[i].trim();
        }
        this.primaryKey = primaryKeys;
        return (R) this;
    }

    /**
     * primaryKey 获取主键名。
     *
     * <pre>
     * 在本方法中为 primaryKey 赋默认值而非构造方法中：
     *  1：提升性能，并不是所有操作都需要 primaryKey 值，且后续 primaryKey(...) 调用可能会赋予另外的值
     *  2：AifeiRow 的子类可通过多态获取到子类对应 table 的主键值
     * </pre>
     */
    public String[] primaryKey() {
        if (primaryKey == null) {
            primaryKey = DbKit.getConfig().getDialect().getDefaultPrimaryKey();
        }
        return primaryKey;
    }

    /**
     * 覆盖内部 Map data 数据，并清除内部 Set change 数据
     */
    public R data(Map<String, Object> data) {
        this.data = data;
        if (change != null) {
            change.clear();
        }
        return (R) this;
    }

    /**
     * 使用 row 对象中的数据覆盖内部 Map data 以及 Set change 数据。用于快速复制 row 对象中的数据，
     * 而不必通过 for 循环逐一赋值（注意线程安全问题）。
     *
     * <p>
     * 注：由于 Dialect 中的 insert、update 生成 SQL 都判断过 if (row.columnDefined(field))，
     *     所以对于生成的 Model，即便复制过来的 Set change 中存在不属于该 Model 的字段，也能正常执行
     *     插入、更新操作。
     */
    public R data(AifeiRow<?> row) {
        this.data = row.data;
        this.change = row.change;
        return (R) this;
    }

    /**
     * 获取内部数据 Map data
     */
    public Map<String, Object> data() {
        if (data == null) {
            data = DbKit.getConfig().dataMapFactory.get();
        }
        return data;
    }

    /**
     * 获取内部数据 Map data 对象内的字段数量。
     * 设计为 size() 而非 isEmpty()，否则被认为是 getter 方法，影响 json 转换。
     */
    public int size() {
        return data != null ? data.size() : 0;
    }

    /**
     * 清除所有数据
     */
    public R clear() {
        if (data != null) {
            data.clear();
        }
        if (change != null) {
            change.clear();
        }
        return (R) this;
    }

    /**
     * 判断 data 中是否拥有 field
     */
    public boolean has(String field) {
        return data().containsKey(field);
    }

    /**
     * 获取字段类型。默认 返回 null，继承类根据情况返回实际类型。
     * 目前用于 Dialect 中根据类型获取自增主键值。
     */
    public Class<?> fieldType(String field) {
        return null;
    }

    /**
     * 获取改变集合。"改变" 集合即 set 系列方法操作涉及的 field 字段名集合。
     * 针对 Row 的 update 方法只会更新该集合内的字段，确保更新操作的数据安全。
     */
    Set<String> change() {
        if (change == null) {
            change = DbKit.getConfig().changeSetFactory.get();
        }
        return change;
    }

    /**
     * 清除改变集合。"改变" 集合即 set 系列方法操作涉及的 field 字段名集合。
     */
    public void clearChange() {
        if (change != null) {
            change.clear();
        }
    }

    /**
     * columnDefined 判断 AifeiRow 子类映射的 table 是否定义了以 field 为值的 column。
     * （check if the column is defined in the table）
     * 子类需覆盖本方法并根据实际情况返回正确的值，不覆盖本方法则默认返回 true。
     *
     * <pre>
     * columnDefined 两种用途：
     *  1: set 系列方法调用该方法返回 true 才允许数据存入，否则抛出异常。需无条件存入数据可使用 put 系列方法。
     *  2: Dialect 生成 insert、update 语句时，调用该方法过滤出来可用于生成的 field 值，以免生成的 field
     *     在 table 中不存在。
     * </pre>
     */
    public boolean columnDefined(String field) {
        return true;    // AifeiRow 为 table 的通用映射，无条件返回 true
    }

    /**
     * 设置字段值，并将 field 添加到 change 集合中去，调用 update 方法
     * 更新到数据库时仅针对 change 集合中的 field 进行更新。
     *
     * <pre>
     * set 系列方法与 put 系列方法的区别：
     *   1: 前者调用 columnDefined 检查 table 是否定义了该 field，而后者不会。
     *   2: 前者向 change 集合中添加 field，而后者不会。
     * </pre>
     *
     * <p>
     * 设计：所有 setXxx 方法都必须转调本方法，利用本方法调用 columnDefined 检查 table
     *      是否定义了该 field，并将 field 添加到 change 集合。
     */
    public R set(String field, Object value) {
        if (!columnDefined(field)) {
            throw new AifeiDbException("Column \"" + field + "\" is not defined in table \"" + table() + "\"");
        }

        data().put(field, value);
        change().add(field);	// Add change flag, update() need this flag.
        return (R) this;
    }

    /**
     * 若 field 在 table 中存在则调用 set(f, v) 存放数据，否则调用 put(f, v) 存放数据。
     *
     * <p>
     * 注意：仅用于子类扩展，在本类中的行为与 set(f, v) 完全一致
     *
     * <pre>
     * 设计：勿提供 setOrPut(Map)、setOrPut(AifeiRow)，否则反序列化时将被误判为 setter 方法
     *      若将来要添加，需添加下划线前缀，如：setOrPut(Map)。
     *
     *      勿移除本方法并转移生成到子类，否则将失去多态优势。
     * </pre>
     */
    public R setOrPut(String field, Object value) {
        if (columnDefined(field)) {
            return set(field, value);
        } else {
            return put(field, value);
        }
    }

    // 仅提供 Cpc.setOrPut(AifeiRow self, Map data) 用法，避免被 json 框架识别为 setter 方法
    R setOrPut(Map<String, Object> data) {
        if (data != null) {
            for (Map.Entry<String, Object> e : data.entrySet()) {
                setOrPut(e.getKey(), e.getValue());
            }
        }
        return (R) this;
    }

    // 仅提供 Cpc.setOrPut(AifeiRow self, AifeiRow dataRow) 用法，避免被 json 框架识别为 setter 方法
    R setOrPut(AifeiRow<?> row) {
        return row != null ? setOrPut(row.data) : (R) this;
    }

    public R set(AifeiRow<?> row) {
        return set(row.data());
    }

    /**
     * 通过 Map 对象设置字段值
     * @param data 存放字段的 Map 对象
     */
    public R set(Map<String, Object> data) {
        if (data != null) {
            for (Map.Entry<String, Object> e : data.entrySet()) {
                // 子类覆盖 set 方法实现字段是否存在于 table 中的检测
                set(e.getKey(), e.getValue());
            }
        }
        return (R) this;
    }

    /**
     * 将 kv 中的几个 setIfXxx 方法拿进来，可以简化下述代码：
     * if (source.get(f) != null) {
     *      target.set(f, source.get(f));
     * }
     * 可简化为: target.setIfNotNull(f, source.get(f));
     */
    public R setIfNotNull(String field, Object value) {
        if (value != null) {
            set(field, value);
        }
        return (R) this;
    }

    public R setIfNotBlank(String field, String value) {
        if (StrUtil.notBlank(value)) {
            set(field, value);
        }
        return (R) this;
    }

    /**
     * 向 data 中添加数据
     * 注意：set 系列方法会向 Map<String> change 集合中添加 field，而 put 系列方法不会。
     */
    public R put(String key, Object value) {
        data().put(key, value);
        return (R) this;
    }

    /**
     * 向 data 中添加来自 row 的 data
     */
    public R put(AifeiRow<?> row) {
        if (row != null) {
            data().putAll(row.data());
        }
        return (R) this;
    }

    public R put(Map<String, Object> data) {
        if (data != null) {
            data().putAll(data);
        }
        return (R) this;
    }

    /**
     * 移除指定字段
     * @param fields 字段名
     */
    public R remove(String... fields) {
        if (fields != null && data != null) {
            for (String field : fields) {
                data.remove(field);
                if (change != null) {
                    change.remove(field);
                }
            }
        }
        return (R) this;
    }

    /**
     * 移除 null 值字段
     */
    public R removeNullValueFields() {
        if (data != null) {
            for (Iterator<Map.Entry<String, Object>> it = data.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Object> entry = it.next();
                if (entry.getValue() == null) {
                    it.remove();
                    if (change != null) {
                        change.remove(entry.getKey());
                    }
                }
            }
        }
        return (R) this;
    }

    /**
     * Keep fields of this Row and remove other fields.
     * @param fields the field names of the Row
     */
    public R keep(String... fields) {
        if (fields == null || fields.length == 0) {
            return clear();
        }
        List<String> fieldList = null;
        if (data != null) {
            fieldList = Arrays.asList(fields);
            data.keySet().retainAll(fieldList);
        }
        if (change != null) {
            if (fieldList == null) {
                fieldList = Arrays.asList(fields);
            }
            change.retainAll(fieldList);
        }
        return (R) this;
    }

    /**
     * 转换成 Row 的子类对象。
     * 返回的子类对象与本对象共享了同一个 Map，需要注意并发等等问题，该方法并未深度 clone
     */
    public <T extends AifeiRow<T>> T to(Class<T> modelClass) {
        T model = InstanceUtil.get(modelClass);
        if (model.table == null) {  // 生成器生成的 model 不改变 table
            model.table = table;
        }
        if (model.primaryKey == null) {
            model.primaryKey = primaryKey;
        }
        model.data = data;
        model.change = change;
        return model;
    }

    /**
     * 获取字段名列表
     */
    public List<String> fieldNames() {
        return new ArrayList<>(data().keySet());
    }

    /**
     * 获取字段值列表
     */
    public List<Object> fieldValues() {
        return new ArrayList<>(data().values());
    }

    public Object getObject(String field) {
        return get(field);
    }

    /**
     * 获取任意类型的字段值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String field) {
        return (T)data().get(field);
    }

    /**
     * 获取任意类型的字段值，值为 null 时返回 defaultValue
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String field, T defaultValue) {
        Object v = get(field);
        return v != null ? (T) v : defaultValue;
    }

    /**
     * 使用类型转换函数转换取到的值
     */
    public <T> T get(String field, Function<Object, T> converter) {
        Object v = get(field);
        return v != null ? converter.apply(v) : null;
    }

    /**
     * 使用类型转换函数转换取到的值
     */
    public <T> T get(String field, T defaultValue, Function<Object, T> converter) {
        Object v = get(field);
        return v != null ? converter.apply(v) : defaultValue;
    }

    /**
     * 获取 String 型字段值。支持的 mysql 字段类型: varchar, char, enum, set, text, tinytext, mediumtext, longtext
     */
    public String getStr(String field) {
        Object v = get(field);
        return v != null ? v.toString() : null;
    }

    public String getStr(String field, String defaultValue) {
        Object v = get(field);
        return v != null ? v.toString() : defaultValue;
    }

    /**
     * 获取 Integer 型字段值。支持的 mysql 字段类型: int, integer, tinyint(n) n > 1, smallint, mediumint
     */
    public Integer getInt(String field) {
        return typeConverter.toInt(get(field));
    }

    public Integer getInt(String field, Integer defaultValue) {
        Object v = get(field);
        return v != null ? typeConverter.toInt(v) : defaultValue;
    }

    /**
     * 获取 Long 型字段值。支持的 mysql 字段类型: bigint, unsigned int
     */
    public Long getLong(String field) {
        return typeConverter.toLong(get(field));
    }

    public Long getLong(String field, Long defaultValue) {
        Object v = get(field);
        return v != null ? typeConverter.toLong(v) : defaultValue;
    }

    /**
     * 获取 Boolean 型字段值。支持的 mysql 字段类型: boolean, tinyint(1), bit
     */
    public Boolean getBoolean(String field) {
        return typeConverter.toBoolean(get(field));
    }

    public Boolean getBoolean(String field, Boolean defaultValue) {
        Object v = get(field);
        return v != null ? typeConverter.toBoolean(v) : defaultValue;
    }

    /**
     * 获取 BigDecimal 型字段值。支持的 mysql 字段类型: decimal, numeric
     */
    public BigDecimal getBigDecimal(String field) {
        return typeConverter.toBigDecimal(get(field));
    }

    public BigDecimal getBigDecimal(String field, BigDecimal defaultValue) {
        Object v = get(field);
        return v != null ? typeConverter.toBigDecimal(v) : defaultValue;
    }

    public BigDecimal getBigDecimal(String field, long defaultValue) {
        Object v = get(field);
        return v != null ? typeConverter.toBigDecimal(v) : BigDecimal.valueOf(defaultValue);
    }

    /**
     * 获取 BigInteger 型字段值。支持的 mysql 字段类型: unsigned bigint
     */
    public BigInteger getBigInteger(String field) {
        return typeConverter.toBigInteger(get(field));
    }

    /**
     * 获取 Date 型字段值。支持的 mysql 字段类型: date, year
     */
    public Date getDate(String field) {
        return typeConverter.toDate(get(field));
    }

    public Date getDate(String field, Date defaultValue) {
        Object v = get(field);
        return v != null ? typeConverter.toDate(v) : defaultValue;
    }

    /**
     * 获取 LocalDateTime 型字段值
     */
    public LocalDateTime getLocalDateTime(String field) {
        return typeConverter.toLocalDateTime(get(field));
    }

    public LocalDateTime getLocalDateTime(String field, LocalDateTime defaultValue) {
        Object v = get(field);
        return v != null ? typeConverter.toLocalDateTime(v) : defaultValue;
    }

    /**
     * 获取 Time 型字段值。支持的 mysql 字段类型: time
     */
    public java.sql.Time getTime(String field) {
        return get(field);
    }

    /**
     * 获取 Timestamp 型字段值。支持的 mysql 字段类型: timestamp, datetime
     */
    public java.sql.Timestamp getTimestamp(String field) {
        return typeConverter.toTimestamp(get(field));
    }

    /**
     * 获取 Double 型字段值。支持的 mysql 字段类型: real, double
     */
    public Double getDouble(String field) {
        return typeConverter.toDouble(get(field));
    }

    /**
     * 获取 Float 型字段值。支持的 mysql 字段类型: float
     */
    public Float getFloat(String field) {
        return typeConverter.toFloat(get(field));
    }

    /**
     * 获取 byte[] 型字段值。支持的 mysql 字段类型: binary, varbinary, tinyblob, blob, mediumblob, longblob
     */
    public byte[] getBytes(String field) {
        return get(field);
    }

    /**
     * 获取任意继承自 Number 类型的字段值
     */
    public Number getNumber(String field) {
        return typeConverter.toNumber(get(field));
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder(30 + size() * 50);

        // table
        ret.append("table = ").append(table);

        // primaryKey
        ret.append("\nprimaryKey = [");
        if (primaryKey != null) {
            for (int i = 0; i < primaryKey.length; i++) {
                if (i > 0) {
                    ret.append(", ");
                }
                ret.append(primaryKey[i]);
            }
        }
        ret.append(']');

        // change
        ret.append("\nchange = [");
        if (change != null) {
            boolean first = true;
            for (String ch : change) {
                if (first) {
                    first = false;
                } else {
                    ret.append(", ");
                }
                ret.append(ch);
            }
        }
        ret.append(']');

        // data
        ret.append("\ndata = {");
        if (data != null) {
            ret.append('\n');
            boolean first = true;
            for (Map.Entry<String, Object> e : data.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    ret.append(",\n");
                }
                Object value = e.getValue();
                if (value != null) {
                    value = value.toString();
                }
                ret.append("  ").append(e.getKey()).append(": ").append(value);
            }
            ret.append('\n');
        }
        return ret.append('}').toString();
    }

    @Override
    public int hashCode() {
        int result = table != null ? table.hashCode() : 0;
        if (primaryKey != null) {
            if (primaryKey.length == 1) {
                result = 31 * result + primaryKey[0].hashCode();
            } else {
                result = 31 * result + Arrays.hashCode(primaryKey);
            }
        }
        result = 31 * result + (data != null ? data.hashCode() : 0);
        result = 31 * result + (change != null ? change.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AifeiRow<?> row = (AifeiRow<?>) o;
        return Objects.equals(table, row.table) && Objects.equals(data, row.data) && Objects.equals(change, row.change) && Arrays.equals(primaryKey, row.primaryKey);
    }

    /**
     * 支持 forEach 方法与 for 语句迭代访问内部数据
     *
     * <pre>
     * 例子：
     *   row.forEach(field -> {
     *       field.getKey();
     *       field.getValue();
     *   });
     *
     * 或者：
     *   for (Map.Entry<String, Object> field : row) {
     *      field.getKey();
     *      field.getValue();
     *   }
     * </pre>
     */
    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return data().entrySet().iterator();
    }
}


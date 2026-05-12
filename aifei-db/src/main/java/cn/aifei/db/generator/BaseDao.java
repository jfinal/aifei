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

package cn.aifei.db.generator;

import cn.aifei.db.core.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * BaseDao 为所有 ModelDao 的父类。
 *
 * <pre>
 * 本类中的方法为 Dao 中带有 table 参数方法去除 table、primary 参数后的实现，
 * 因为这两个参数在 model 中是已知的，消除 table、primaryKey 参数，进一步
 * 提升开发体验，提高开发效率。
 * </pre>
 */
public abstract class BaseDao<D extends AifeiDao<D, R>, R extends AifeiRow<R>> extends AifeiDao<D, R> {

    private final Table table;

    @SuppressWarnings("unchecked")
    public BaseDao(Table table) {
        super((Class<R>) table.modelType);
        this.table = table;
    }

    /**
     * 强行(改变)配置
     */
    public BaseDao<D, R> forceConfig(String configId) {
        DbConfig config = DbKit.getConfig(configId);
        if (config == null) {
            throw new RuntimeException("config can not be null");
        }
        this.config = config;
        return this;
    }

    // -----------------------------------------------------------------------------------------------------------

    // 以下方法为 Dao 中带有 table 参数方法去除 table、primary 参数后的实现，因为这两个参数在 model 中是已知的
    // 消除 table、primaryKey 参数，进一步提升开发体验，提高开发效率

    /**
     * 通过主键删除
     * <p>
     * 例子：User.deleteById(123)
     */
    public boolean deleteById(Object id) {
        if (table.primaryKey.length != 1) {
            throw new IllegalStateException("deleteById method requires exactly 1 primary key but found " + table.primaryKey.length + ".");
        }
        return config.getDeleteExecutor().deleteById(this, table.name, table.primaryKey[0], id);
    }

    /**
     * 通过复合主键删除。仅支持复合主键数量为 2 的 table。绝大多数复合主键只有两个
     * <p>
     * 考虑：当 Model 对应的 table 没有复合主键时在 baseMode 中生成本方法并将其 @Deprecated
     *
     * @param key1 第一个复合主键名
     * @param key2 第二个复合主键名
     * @param id1  第一个复合主键值
     * @param id2  第二个复合主键值
     */
    public boolean deleteByCompositeId(String key1, String key2, Object id1, Object id2) {
        return config.getDeleteExecutor().deleteByCompositeId(this, table.name, new String[]{key1, key2}, new Object[]{id1, id2});
    }

    /**
     * 通过复合主键删除。支持任意数量的复合主键
     */
    public boolean deleteByCompositeId(String[] compositeId, Object[] idValues) {
        return config.getDeleteExecutor().deleteByCompositeId(this, table.name, compositeId, idValues);
    }

    /**
     * 例子：User.deleteBy("age >= ? and age <= ?", 18, 25);
     */
    public int deleteBy(String whereOrField, Object... paraArray) {
        return config.getDeleteExecutor().deleteBy(this, table.name, whereOrField, paraArray);
    }

    /**
     * 删除指定 id 值的数据
     * <p>
     * 例子：User.deleteInIds(Arrays.asList(1, 2, 3))
     */
    public int deleteInIds(Collection<?> ids) {
        return config.getDeleteExecutor().deleteIn(this, table.name, table.primaryKey[0], ids);
    }

    /**
     * 删除指定 id 值的数据
     * <p>
     * 例子：User.deleteInIds(1, 2, 3)
     */
    public int deleteInIds(Object... idValues) {
        return config.getDeleteExecutor().deleteIn(this, table.name, table.primaryKey[0], Arrays.asList(idValues));
    }

    /**
     * 删除指定字段所包含值的数据
     * <p>
     * 例子：User.deleteIn("name", Arrays.asList("james", "jason"))
     */
    public int deleteIn(String field, Collection<?> fieldValues) {
        return config.getDeleteExecutor().deleteIn(this, table.name, field, fieldValues);
    }

    /**
     * 删除指定字段所包含值的数据
     * <p>
     * 例子：User.deleteIn("name", "james", "jason")
     */
    public int deleteIn(String field, Object... fieldValues) {
        return config.getDeleteExecutor().deleteIn(this, table.name, field, Arrays.asList(fieldValues));
    }

    /**
     * 例子：User.select("name, age").findAll();
     * <p>
     * 注意：不要在 BaseUser 中提供 static finaAll()，因为要鼓励使用 select(fields)
     */
    public List<R> findAll() {
        return config.getFindExecutor().findAll(this, table.name);
    }

    /**
     * 通过主键查询
     * <p>
     * 例子：User.findById(123)
     */
    public R findById(Object id) {
        if (table.primaryKey.length != 1) {
            throw new IllegalStateException("findById method requires exactly 1 primary key but found " + table.primaryKey.length + ".");
        }
        return config.getFindExecutor().findById(this, table.name, table.primaryKey, new Object[]{id});
    }

    /**
     * 通过复合主键查询。仅支持复合主键数量为 2 的 table。绝大多数复合主键只有两个
     * <p>
     * 考虑：当 Model 对应的 table 没有复合主键时在 baseMode 中生成本方法并将其 @Deprecated
     *
     * @param key1 第一个复合主键名
     * @param key2 第二个复合主键名
     * @param id1  第一个复合主键值
     * @param id2  第二个复合主键值
     */
    public R findByCompositeId(String key1, String key2, Object id1, Object id2) {
        return config.getFindExecutor().findById(this, table.name, new String[]{key1, key2}, new Object[]{id1, id2});
    }

    /**
     * 通过复合主键查询。支持任意数量的复合主键
     */
    public R findByCompositeId(String[] compositeId, Object[] idValues) {
        return config.getFindExecutor().findById(this, table.name, compositeId, idValues);
    }

    /**
     * findBy 可让单表查询 sql 省去除了 where 之外的其它部分，提升开发体验
     *
     * <pre>
     * 例子：
     *     User.select("*").findBy("age = ? order by id desc", 18);
     *     User.select("*").findBy("age = 18 order by id desc");
     *     User.select("id, name").findBy("age >= ? and age <= ? order by age", 18, 25);
     * </pre>
     */
    public List<R> findBy(String whereOrField, Object... paraArray) {
        return config.getFindExecutor().findBy(this, table.name, whereOrField, paraArray);
    }

    /**
     * findFirstBy 可让单表查询 sql 省去除了 where 之外的其它部分，提升开发体验
     * 与 findBy 参数用法完全一致，但仅返回首条数据
     * <p>
     * 例子：User.select("id, name, age").findFirstBy("age >= ? and sex = ?", 18, "女");
     */
    public R findFirstBy(String whereOrField, Object... paraArray) {
        return config.getFindExecutor().findFirstBy(this, table.name, whereOrField, paraArray);
    }

    /**
     * 例子：User.select("id, name, age").findInIds(Arrays.asList(1, 2, 3))
     */
    public List<R> findInIds(Collection<?> ids) {
        return config.getFindExecutor().findIn(this, table.name, table.primaryKey[0], ids);
    }

    /**
     * 例子：User.select("id, name, age").findInIds(1, 2, 3)
     */
    public List<R> findInIds(Object... idValues) {
        return config.getFindExecutor().findIn(this, table.name, table.primaryKey[0], Arrays.asList(idValues));
    }

    /**
     * 例子：User.select("id, name, age").findIn("age", Arrays.asList(18, 19, 20))
     */
    public List<R> findIn(String field, Collection<?> fieldValues) {
        return config.getFindExecutor().findIn(this, table.name, field, fieldValues);
    }

    /**
     * 例子：User.select("id, name, age").findIn("age", 18, 19, 20)
     */
    public List<R> findIn(String field, Object... fieldValues) {
        return config.getFindExecutor().findIn(this, table.name, field, Arrays.asList(fieldValues));
    }

    /**
     * 例子：User.count();
     */
    public long count() {
        return config.getQueryExecutor().count(this, table.name);
    }

    /**
     * 例子：User.countBy("sex = ? and age = ?", "女", 18);
     */
    public long countBy(String whereOrField, Object... paraArray) {
        return config.getQueryExecutor().countBy(this, table.name, whereOrField, paraArray);
    }
}


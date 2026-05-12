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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Dao 对象非线程安全，多线程不能共享
 */
public class Dao extends AifeiDao<Dao, Row> {

    public Dao(DbConfig config) {
        super(config, Row.class);
    }

    // ------------------------------------------------------------------------------------
    // AifeiDao 中所有带 table 参数的方法转移至此

    public boolean deleteById(String table, Object id) {
        String primaryKey = config.dialect.getDefaultPrimaryKey()[0];
        return config.deleteExecutor.deleteById(this, table, primaryKey, id);
    }

    public boolean deleteById(String table, String primaryKey, Object id) {
        return config.deleteExecutor.deleteById(this, table, primaryKey, id);
    }

    /**
     * 通过复合主键删除。仅支持复合主键数量为 2 的 table。绝大多数复合主键只有两个
     * @param key1 第一个复合主键名
     * @param key2 第二个复合主键名
     * @param id1 第一个复合主键值
     * @param id2 第二个复合主键值
     */
    public boolean deleteByCompositeId(String table, String key1, String key2, Object id1, Object id2) {
        return config.deleteExecutor.deleteByCompositeId(this, table, new String[]{key1, key2}, new Object[]{id1, id2});
    }

    /**
     * 通过复合主键删除。支持任意数量的复合主键
     * @param compositeId 复合主键名
     * @param idValues 复合主键值，主键值次序要与主键名保持一致
     */
    public boolean deleteByCompositeId(String table, String[] compositeId, Object[] idValues) {
        return config.deleteExecutor.deleteByCompositeId(this, table, compositeId, idValues);
    }

    public int deleteBy(String table, String whereOrField, Object... paraArray) {
        return config.deleteExecutor.deleteBy(this, table, whereOrField, paraArray);
    }

    // 去除该方法，因为 deleteBy("age > ?", 18) 这种明明是调用 where 参数的场景错误的调用了本方法，虽然结果是对的
    // public int deleteBy(String table, String field, Object value) {
    //     return config.deleteExecutor.deleteBy(this, table, field, new Object[]{value});
    // }

    public int deleteInIds(String table, Collection<?> ids) {
        String primaryKey = config.dialect.getDefaultPrimaryKey()[0];
        return config.deleteExecutor.deleteIn(this, table, primaryKey, ids);
    }

    public int deleteInIds(String table, Object... idValues) {
        String primaryKey = config.dialect.getDefaultPrimaryKey()[0];
        return config.deleteExecutor.deleteIn(this, table, primaryKey, Arrays.asList(idValues));
    }

    public int deleteIn(String table, String field, Collection<?> fieldValues) {
        return config.deleteExecutor.deleteIn(this, table, field, fieldValues);
    }

    public int deleteIn(String table, String field, Object... fieldValues) {
        return config.deleteExecutor.deleteIn(this, table, field, Arrays.asList(fieldValues));
    }

    /**
     * 获取 table 中所有数据
     */
    public List<Row> findAll(String table) {
        return config.findExecutor.findAll(this, table);
    }

    /**
     * 通过 id 查询。主键名 primaryKey 通过 Dialect.getDefaultPrimaryKey() 获取
     */
    public Row findById(String table, Object id) {
        return config.findExecutor.findById(this, table, config.getDialect().getDefaultPrimaryKey(), new Object[]{id});
    }

    /**
     * 通过 id 查询
     */
    public Row findById(String table, String primaryKey, Object id) {
        return config.findExecutor.findById(this, table, new String[]{primaryKey}, new Object[]{id});
    }

    /**
     * 通过复合主键查询。仅支持复合主键数量为 2 的 table。绝大多数复合主键只有两个
     * @param key1 第一个复合主键名
     * @param key2 第二个复合主键名
     * @param id1 第一个复合主键值
     * @param id2 第二个复合主键值
     */
    public Row findByCompositeId(String table, String key1, String key2, Object id1, Object id2) {
        return config.findExecutor.findById(this, table, new String[]{key1, key2}, new Object[]{id1, id2});
    }

    /**
     * 通过复合主键查询。支持任意数量的复合主键
     * @param compositeId 复合主键名
     * @param idValues 复合主键值，主键值次序要与主键名保持一致
     */
    public Row findByCompositeId(String table, String[] compositeId, Object[] idValues) {
        return config.findExecutor.findById(this, table, compositeId, idValues);
    }

    /**
     * findBy 可让 model 单表查询 sql 省去除了 where 之外的其它部分，提升开发体验
     *
     * <pre>
     * 例子：
     *  Db 用法需传入参数 table
     *     Db.select("*").findBy("user", "name", "james);
     *     Db.select("*").findBy("user", "id > ? and age = ?", 5, 18);
     *
     *  Model 用法可省去参数 table
     *     User.select("*").findBy("age", 18);
     *     User.select("*").findBy("age = 18");
     *     User.select("*").findBy("age = ? order by id desc", 18);
     *     User.select("id, name").findBy("age >= ? and age <= ?", 18, 25);
     * </pre>
     */
    public List<Row> findBy(String table, String whereOrField, Object... paraArray) {
        return config.findExecutor.findBy(this, table, whereOrField, paraArray);
    }

    /**
     * 去除该方法，因为 findBy("age > ?", 18) 这种明明是调用 where 参数的场景错误的调用了本方法，虽然结果是对的
     * 解决单字段条件查询调用到错误方法的问题
     * 用法：User.select("*").findBy("name", "james")
     */
    // public List<Row> findBy(String table, String field, Object value) {
    //     return config.findExecutor.findBy(this, table, field, new Object[]{value});
    // }

    /**
     * findFirstBy 与 findBy 参数用法完全一致，但仅返回首条数据
     */
    public Row findFirstBy(String table, String whereOrField, Object... paraArray) {
        return config.findExecutor.findFirstBy(this, table, whereOrField, paraArray);
    }

    /**
     * 去除该方法，因为 findFirstBy("age > ?", 18) 这种明明是调用 where 参数的场景错误的调用了本方法，虽然结果是对的
     * 解决单字段条件查询调用到错误方法的问题
     * 用法：User.select("*").findFirstBy("name", "james")
     */
    // public Row findFirstBy(String table, String field, Object value) {
    //     return config.findExecutor.findFirstBy(this, table, field, new Object[]{value});
    // }

    /**
     * 生成 select ... from table where id in(...) 进行查询。
     * <p>
     * 注意：不支持复合主键
     */
    public List<Row> findInIds(String table, Collection<?> ids) {
        String primaryKey = config.dialect.getDefaultPrimaryKey()[0];
        return config.findExecutor.findIn(this, table, primaryKey, ids);
    }

    /**
     * 生成 select ... from table where id in(...) 进行查询。
     * <p>
     * 注意：不支持复合主键
     */
    public List<Row> findInIds(String table, Object... idValues) {
        String primaryKey = config.dialect.getDefaultPrimaryKey()[0];
        return config.findExecutor.findIn(this, table, primaryKey, Arrays.asList(idValues));
    }

    /**
     * 生成 select ... from table where field in(...) 进行查询。
     */
    public List<Row> findIn(String table, String field, Collection<?> fieldValues) {
        return config.findExecutor.findIn(this, table, field, fieldValues);
    }

    /**
     * 生成 select ... from table where field in(...) 进行查询。
     */
    public List<Row> findIn(String table, String field, Object... fieldValues) {
        return config.findExecutor.findIn(this, table, field, Arrays.asList(fieldValues));
    }

    // 去除 queryBy 方法，对比直接使用 Db.sql(...).query() 并没有节省代码，反而提高了学习成本与认知负载
    // queryBy 方法 ----------------------------------------------------------------------
    // /**
    //  * queryBy 查询与 findBy 功能类似，但是查询结果不封装为 Row 对象
    //  *
    //  * <pre>
    //  * 例子：
    //  *     List<Object[]> = Db.select("id, name").queryBy("user", "name", "Aifei");
    //  *     List<Object[]> = Db.select("count(*), age").queryBy("user", "age >= ? group by age", 18);
    //  * </pre>
    //  */
    // public <T> List<T> queryBy(String table, String whereOrField, Object... paraArray) {
    //     return config.queryExecutor.queryBy(this, table, whereOrField, paraArray);
    // }

    // /**
    //  * 去除该方法，因为 queryBy("age > ?", 18) 这种明明是调用 where 参数的场景错误的调用了本方法，虽然结果是对的
    //  * queryBy 查询与 findBy 功能类似，但是查询结果不封装为 Row 对象
    //  * <p>
    //  * 解决单字段条件查询调用到错误方法的问题
    //  *
    //  * <pre>
    //  * 例子：
    //  *     List<Object[]> = Db.select("id, name").queryBy("user", "name", "james")
    //  * </pre>
    //  */
    // public <T> List<T> queryBy(String table, String field, Object value) {
    //     return config.queryExecutor.queryBy(this, table, field, new Object[]{value});
    // }

    // 其它查询 ----------------------------------------------------------------------------

    /**
     * 用法：Db.count("user");
     */
    public long count(String table) {
        return config.queryExecutor.count(this, table);
    }

    /**
     * 用法：Db.countBy("user", "sex = ? and age = ?", "女", 18);
     */
    public long countBy(String table, String whereOrField, Object... paraArray) {
        return config.queryExecutor.countBy(this, table, whereOrField, paraArray);
    }

    /**
     * 去除该方法，因为 countBy("age > ?", 18) 这种明明是调用 where 参数的场景错误的调用了本方法，虽然结果是对的
     * 用法：Db.countBy("user", "age", 18);
     */
    // public long countBy(String table, String field, Object value) {
    //     return config.queryExecutor.countBy(this, table, field, new Object[]{value});
    // }
}




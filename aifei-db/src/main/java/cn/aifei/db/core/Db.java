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

import cn.aifei.db.executor.FunExecutor.*;
import cn.aifei.db.transaction.Atom;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Db + Row 数据库操作神器
 */
public class Db {

    private Db() {}

    /**
     * 使用默认配置集合(包含默认数据源)创建 Dao 对象，随后链式调用操作数据库
     */
    public static Dao use() {
        return DbKit.getConfig().createDao();
    }

    /**
     * 通过 configId 指定配置集合(包含数据源)并创建 Dao 对象，随后链式调用操作数据库
     */
    public static Dao use(String configId) {
        return DbKit.getConfig(configId).createDao();
    }

    // 新增 ----------------------------------------------------------------------------

    /**
     * 插入数据
     * <p>
     * 注意：insert 操作要么成功，要么抛出异常，所以多数场景无需对操作成功与否进行判断
     *
     * <pre>
     * 例子：
     *    Db.insert(Row.of("user").set("name", "james"));                   // 默认主键 "id"
     *    Db.insert(Row.of("user", "user_id").set("name", "james"));        // 指定主键 "user_id"
     *    Db.insert(Row.of("user_role", "user_id", "role_id").id(12, 3));   // 复合主键 "user_id"、"role_id"
     * </pre>
     */
    public static Row insert(Row row) {
        return use().insert(row);
    }

    /**
     * 不存在主键时插入，否则更新
     */
    public static Row insertOrUpdate(Row row) {
        return use().insertOrUpdate(row);
    }

    // 删除 ----------------------------------------------------------------------------

    /**
     * 通过 Row 对象中的 table 与 id 值删除数据
     *
     * <pre>
     * 例子：
     *    Db.delete(Row.of("user").id(123));               // 默认主键 "id"
     *    Db.delete(Row.of("user", "user_id").id(123));    // 指定主键 "user_id"
     * </pre>
     */
    public static boolean delete(Row row) {
        return use().delete(row);
    }

    /**
     * 通过 table 与 id 值删除数据，主键名默认使用 "id"
     *
     * <pre>
     * 例子：
     *    Db.deleteById("user", 123);   // 默认主键名为 "id"
     * </pre>
     */
    public static boolean deleteById(String table, Object id) {
        return use().deleteById(table, id);
    }

    /**
     * 通过 table 与 id 值删除数据
     *
     * <pre>
     * 例子：
     *    Db.deleteById("article", "article_id", 123);   // 指定主键名 "article_id"
     * </pre>
     */
    public static boolean deleteById(String table, String primaryKey, Object id) {
        return use().deleteById(table, primaryKey, id);
    }

    public static boolean deleteByCompositeId(String table, String key1, String key2, Object id1, Object id2) {
        return use().deleteByCompositeId(table, key1, key2, id1, id2);
    }

    public static boolean deleteByCompositeId(String table, String[] compositeId, Object[] idValues) {
        return use().deleteByCompositeId(table, compositeId, idValues);
    }

    public static int deleteBy(String table, String whereOrField, Object... paraArray) {
        return use().deleteBy(table, whereOrField, paraArray);
    }

    public static int deleteInIds(String table, Collection<Object> ids) {
        return use().deleteInIds(table, ids);
    }

    public static int deleteInIds(String table, Object... idValues) {
        return use().deleteInIds(table, idValues);
    }

    public static int deleteIn(String table, String field, Collection<Object> fieldValues) {
        return use().deleteIn(table, field, fieldValues);
    }

    public static int deleteIn(String table, String field, Object... fieldValues) {
        return use().deleteIn(table, field, fieldValues);
    }

    // 更新 ----------------------------------------------------------------------------

    /**
     * 通过 Row 对象更新数据，更新条件为 table 与 id 值
     *
     * <pre>
     * 例子：
     *    Db.update(Row.of("user").id(123).set("age", 18));               // 默认主键 "id"
     *    Db.update(Row.of("user", "user_id").id(123).set("age", 18));    // 指定主键 "user_id"
     * </pre>
     */
    public static boolean update(Row row) {
        return use().update(row);
    }

    // 查询 ----------------------------------------------------------------------------

    /**
     * Enjoy sql 操作数据库
     * 1: 使用 enjoy sql + paras 操作数据库
     * 2: 通过 sql 生成 sqlId 作为 key 缓存该 enjoy sql
     * 3: 可在 sql 中指定 sqlId，从而避免生成 sqlId 来提升性能
     *
     * <pre>
     * sql 中指定 sqlId 的例子：
     *    String sql = "|user.findById| select * from user where id = ?"
     *    Db.sql(sql, 123).find();
     * 以上 sql 指定了 sqlId 值 "user.findById" 并通过分隔字符 '|' 将其与其它部分分隔开来
     *
     * 分隔字符前后允许存在一个或多个空格，例如：
     *    "| user.findById | select * from user where id = ?"
     *
     * 此外，还可以通过配置 DbConfig.idSqlFactory 来定制 sqlId 分割策略
     * </pre>
     *
     * <pre>
     *  高级用法：
     *    在 enjoy sql 中使用 #define 指令：
     *    String sql = "#define select() " +
     *                     "select id, age, name " +
     *                  "#end " +
     *                  "#@select() from user";
     *    Db.sql(sql).find();
     *
     *  同理：#include 等等指令也必然支持：
     *       假定 inc.txt 内容为: select id, name
     *       String sql = "#include('inc.txt') from user ";
     *       Db.sql(sql).find();
     *
     *  注意：所有的支持 enjoy sql 的方法(sql 系、sqlId 系)都支持在 enjoy sql 使用 #define 指令，
     *       并且还可以使用 engine.addSharedFunction(...) 添加共享函数，然后可以在所有 enjoy sql
     *       中使用它，极大提升开发体验
     * </pre>
     *
     * @param sql enjoy sql 或原生 sql
     * @param paras #para(int) 指令与问号占位字符所对应的参数
     */
    public static Dao sql(String sql, Object... paras) {
        return use().sql(sql, paras);
    }

    /**
     * Enjoy sql 操作数据库
     * 1: 使用 enjoy sql 操作数据库
     * 2: 通过 sql 生成 sqlId 作为 key 缓存该 enjoy sql
     * 3: 可在 sql 中指定 sqlId，从而避免生成 sqlId 来提升性能
     *
     * <pre>
     * sql 中指定 sqlId 的例子：
     *    String sql = "|user.findJames| select * from user where name = 'james'"
     *    Db.sql(sql).find();
     * 以上 sql 指定了 sqlId 值 "user.findJames" 并通过分隔字符 '|' 将其与其它部分分隔开来
     *
     * 分隔字符前后允许存在一个或多个空格，例如：
     *    "| user.findById | select * from user where id = 123"
     *
     * 此外，还可以通过配置 DbConfig.idSqlFactory 来定制 sqlId 分割策略
     * </pre>
     *
     * @param sql enjoy sql 或原生 sql
     */
    public static Dao sql(String sql) {
        return use().sql(sql, DbKit.EMPTY_OBJECT_ARRAY);
    }

    /**
     * Enjoy sql 操作数据库
     * 1: 使用 enjoy sql + data 操作数据库
     * 2: 通过 sql 生成 sqlId 作为 key 缓存该 enjoy sql
     * 3: 可在 sql 中指定 sqlId，从而避免生成 sqlId 来提升性能
     *
     * <pre>
     * sql 中指定 sqlId 的例子：
     *    String sql = "|user.findById| select * from user where id = ?"
     *    Db.sql(sql, 123).find();
     * 以上 sql 指定了 sqlId 值 "user.findById" 并通过分隔字符 '|' 将其与其它部分分隔开来
     *
     * 分隔字符前后允许存在一个或多个空格，例如：
     *    "| user.findById | select * from user where id = ?"
     *
     * 此外，还可以通过配置 DbConfig.idSqlFactory 来定制 sqlId 分割策略
     * </pre>
     *
     * @param sql enjoy sql 或原生 sql
     * @param data #para(name) 与 #(name) 等 enjoy 指令使用的数据
     */
    public static Dao sql(String sql, Map<?, ?> data) {
        return use().sql(sql, data);
    }

    /**
     * 通过 sqlId 获取外部文件中的 sql，随后的链式调用操作数据库
     * <p>
     * 注意：通过外部文件添加的 sql 需要使用 #sql(...) 指令指定 sqlId
     *
     * @param sqlId 作为 key 获取外部文件中的 sql
     * @param paras #para(int) 指令与问号占位字符所对应的参数
     */
    public static Dao sqlById(String sqlId, Object... paras) {
        return use().sqlById(sqlId, paras);
    }

    /**
     * 通过 sqlId 获取外部文件中的 sql，随后的链式调用操作数据库
     * <p>
     * 注意：通过外部文件添加的 sql 需要使用 #sql(...) 指令指定 sqlId
     *
     * @param sqlId 作为 key 获取外部文件中的 sql
     */
    public static Dao sqlById(String sqlId) {
        return use().sqlById(sqlId, DbKit.EMPTY_OBJECT_ARRAY);
    }

    /**
     * 通过 sqlId 获取外部文件中的 sql，随后的链式调用操作数据库
     * <p>
     * 注意：通过外部文件添加的 sql 需要使用 #sql(...) 指令指定 sqlId
     *
     * @param sqlId 作为 key 获取外部文件中的 sql
     * @param data #para(name) 与 #(name) 等 enjoy 指令使用的数据
     */
    public static Dao sqlById(String sqlId, Map<?, ?> data) {
        return use().sqlById(sqlId, data);
    }

    /**
     * 通过纯 sql + para 操作数据库，不使用 enjoy sql
     */
    public static Dao sqlPara(String sql, Object... paras) {
        return use().sqlPara(sql, paras);
    }

    /**
     * 为所有未使用 select 子句的 sql 查询指定返回字段，避免 "select *" 返回所有字段，从而提升性能
     *
     * <pre>
     * 例如：
     *    Db.select("name, age").findById("user", 123);
     *    User.select("name, age").findById(123);
     *
     * 支持别名：
     *    Db.select("user_id AS userId, title").findById("article", 456);
     *    Article.select("user_id AS userId, title").findById(456);
     *
     * 如果不使用 select，findById、findAll 这类方法将默认使用 "select *" 返回所有字段，例如：
     *    Db.findById("article", 789);
     *    Article.findById(789);
     *    以上查询即便仅需用到 id、title 字段值时，content 这个大字段也会被加载，不仅浪费空间而且拉低性能
     * </pre>
     */
    public static Dao select(String fields) {
        return use().select(fields);
    }

    // /**
    //  * 获取 table 中所有数据
    //  *
    //  * <p>
    //  * 注意：不提供 findAll，而是提倡使用：Db.select("id, name, ...").findAll(table) 避免获取所有字段
    //  */
    // public static List<Row> findAll(String table) {
    //     return use().findAll(table);
    // }

    public static Row findById(String table, Object id) {
        return use().findById(table, id);
    }

    public static Row findById(String table, String primaryKey, Object id) {
        return use().findById(table, primaryKey, id);
    }

    public static Row findByCompositeId(String table, String key1, String key2, Object id1, Object id2) {
        return use().findByCompositeId(table, key1, key2, id1, id2);
    }

    public static Row findByCompositeId(String table, String[] compositeId, Object[] idValues) {
        return use().findByCompositeId(table, compositeId, idValues);
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
    public static List<Row> findBy(String table, String whereOrField, Object... paraArray) {
        return use().findBy(table, whereOrField, paraArray);
    }

    /**
     * findFirstBy 与 findBy 参数用法完全一致，但仅返回首条数据
     */
    public static Row findFirstBy(String table, String whereOrField, Object... paraArray) {
        return use().findFirstBy(table, whereOrField, paraArray);
    }

    public static List<Row> findInIds(String table, Collection<Object> ids) {
        return use().findInIds(table, ids);
    }

    public static List<Row> findInIds(String table, Object... idValues) {
        return use().findInIds(table, idValues);
    }

    /**
     * 生成 select ... from table where field in(...) 进行查询。
     */
    public static List<Row> findIn(String table, String field, Collection<Object> fieldValues) {
        return use().findIn(table, field, fieldValues);
    }

    /**
     * 生成 select ... from table where field in(...) 进行查询。
     */
    public static List<Row> findIn(String table, String field, Object... fieldValues) {
        return use().findIn(table, field, fieldValues);
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
    // public static <T> List<T> queryBy(String table, String whereOrField, Object... paraArray) {
    //     return use().queryBy(table, whereOrField, paraArray);
    // }

    // 其它 ----------------------------------------------------------------------------

    /**
     * 用法：Db.count("user");
     */
    public static long count(String table) {
        return use().count(table);
    }

    /**
     * 用法：Db.countBy("user", "sex = ? and age = ?", "女", 18);
     */
    public static long countBy(String table, String whereOrField, Object... paraArray) {
        return use().countBy(table, whereOrField, paraArray);
    }

    // batch 操作 --------------------------------------------------------------------------

    /**
     * 使用默认 config 批量操作
     * <p>
     * 注意：优先使用 Row 对象中的 table 值，当该值不存在时使用 table 参数值
     */
    public static Batch batch() {
        return DbKit.getConfig().createBatch();
    }

    /**
     * 使用指定 config 批量操作
     */
    public static Batch batch(String configId) {
        return DbKit.getConfig(configId).createBatch();
    }

    /**
     * 批量插入
     */
    public static <T extends AifeiRow<T>> BatchResult batchInsert(List<T> rowList) {
        return batch().insert(rowList);
    }

    /**
     * 指定 table 批量插入。Row 对象内的 table 优先级高于当前方法参数 table 参数
     */
    public static <T extends AifeiRow<T>> BatchResult batchInsert(String table, List<T> rowList) {
        return batch().insert(table, rowList);
    }

    /**
     * 批量更新
     */
    public static <T extends AifeiRow<T>> BatchResult batchUpdate(List<T> rowList) {
        return batch().update(rowList);
    }

    /**
     * 指定 table 批量更新。Row 对象内的 table 优先级高于当前方法参数 table 参数
     */
    public static <T extends AifeiRow<T>> BatchResult batchUpdate(String table, List<T> rowList) {
        return batch().update(table, rowList);
    }

    /**
     * 使用原生 sql + para 进行批量操作，不依赖 Row 对象
     */
    public static BatchResult batchExecute(String sql, List<Object[]> parasList) {
        return batch().execute(sql, parasList);
    }

    /**
     * 使用原生 sql + para 进行批量操作，不依赖 Row 对象
     */
    public static BatchResult batchExecute(String sql, Object[][] parasArray) {
        return batch().execute(sql, parasArray);
    }

    /**
     * 批量执行 sql
     */
    public static BatchResult batchExecute(List<String> sqlList) {
        return batch().execute(sqlList);
    }

    // 底层 JDBC ----------------------------------------------------------------------------

    /**
     * 使用底层 JDBC 操作数据库，用完 Connection 后无需调用 connection.close()（原则：谁获取谁关闭）
     * 常用于调用存储过程，或者需要使用 JDBC 的场景（例如测试 JDBC 是否可用）
     *
     * <pre>
     * 调用存储过程 my_procedure 举例：
     *  Db.call((conn, kit) -> {
     *    Out out = new Out();
     *    try (CallableStatement cs = conn.prepareCall("{CALL my_procedure(?,?)}")) {
     *      cs.setObject(1, para1);
     *      cs.setObject(2, para2);
     *      try (ResultSet rs = cs.executeQuery()) {
     *        return kit.getRowList(rs);
     *      }
     *    }
     *  });
     * </pre>
     */
    public static <R> R call(JdbcFun<R> fun) {
        return use().call(fun);
    }

    // 事务 ----------------------------------------------------------------------------

    /**
     * 事务
     *
     * <pre>
     * 例子:
     * Db.transaction(tx -> {
     *     // 两个账户之间转账 100 元
     *     String sql = "update account set balance = balance + ? where id = ?";
     *     int money = 100;
     *
     *     int n1 = Db.sql(sql, -money, from).update();     // 余额减去 100
     *     int n2 = Db.sql(sql, money, to).update();        // 余额加上 100
     *
     *     if (n1 == 1 && n2 == 1) {
     *         return Out.ok("转账成功");              // 未调用 tx.rollback() 并且未抛出异常，自动提交事务
     *     } else {
     *         tx.rollback();                         // 调用 tx.rollback() 或者抛出异常，回滚事务
     *         return Out.fail("转账失败");
     *     }
     * });
     * </pre>
     */
    public static <R> R transaction(Atom<R> atom) {
        return use().transaction(atom);
    }
}




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

import cn.aifei.db.executor.FunExecutor.JdbcFun;
import cn.aifei.db.factory.DataMapFactory;
import cn.aifei.db.transaction.Atom;
import cn.aifei.db.transaction.Isolation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

/**
 * AifeiDao 结合 Row 通过 DbConfig 中的 Executor 等组件操作数据库。
 * <p>
 * AifeiDao 为 Dao、BaseDao 的父类，AifeiRow 为 Row、BaseModel 的父类。
 *
 * <p>
 * 注意：AifeiDao 对象非线程安全，多线程不能共享
 */
@SuppressWarnings({"unchecked"})
public class AifeiDao<D extends AifeiDao<D, R>, R extends AifeiRow<R>> {

    protected DbConfig config;
    protected SqlPara sqlPara;
    protected Class<R> rowType;

    protected String select;
    protected Boolean hasGroupBy;
    protected DataMapFactory dataMapFactory;

    public AifeiDao(Class<R> rowType) {
        this(DbKit.getConfig(rowType), rowType);
    }

    public AifeiDao(DbConfig config, Class<R> rowType) {
        if (config == null) {
            throw new IllegalArgumentException("config can not be null.");
        }
        if (rowType == null) {
            throw new IllegalArgumentException("rowType can not be null.");
        }
        this.config = config;
        this.rowType = rowType;
    }

    // public AifeiDao() {
    //     config = DbKit.getConfig();     // 非 null 不转调 this(...)
    //     rowType = (Class<R>) Row.class;
    // }
    // public AifeiDao(DbConfig config) {
    //     this(config, (Class<R>) Row.class);
    // }
    // public AifeiDao(String configId) {
    //     this(DbKit.getConfig(configId), (Class<R>) Row.class);
    // }

    /**
     * 不要提供 config(String configId)，不支持 Dao 创建之后切换 config
     */
    public DbConfig config() {
        return config;
    }

    // Dao + Row 与 UserDao + User 类型都是确定的，不支持指定类型
    // public D rowType(Class<R> rowType) {
    //     if (rowType == null) {
    //         throw new IllegalArgumentException("rowType can not be null.");
    //     }
    //     this.rowType = rowType;
    //     return (D) this;
    // }

    public Class<R> rowType() {
        return rowType;
    }

    /**
     * 为当前查询指定 DataMapFactory 实现类。当前指定值优先于 DbConfig 中的 DataMapFactory
     *
     * <pre>
     * 例子：
     *    Db.sql("select id, name, title from user").dataMapFactory(HashDataMapFactory.instance).find();
     *
     *    HashDataMapFactory 在性能和内存占用上略优于 DataMapFactory，可用于数据量大但不关心 select 字段顺序的场景
     * </pre>
     */
    public D dataMapFactory(DataMapFactory dataMapFactory) {
        this.dataMapFactory = dataMapFactory;
        return (D) this;
    }

    /**
     * 优先使用临时指定的 DataMapFactory
     */
    public DataMapFactory dataMapFactory() {
        return dataMapFactory != null ? dataMapFactory : config.dataMapFactory;
    }

    /**
     * 插入数据。insert 操作要么成功，要么抛出异常，无需对操作成功与否进行判断。
     *
     * <pre>
     * 返回值类型未设计成 boolean 的原因：
     *   1: insert into 语句在插入失败时会抛出异常，没有返回值，设计成 boolean 没有意义
     *   2: 即便通过 try catch 将异常捕捉并 return false，但会掩盖异常原因，既有安全隐患也不利于排错
     *   重点：插入操作，要么成功，要么抛出异常。
     *
     * 例子：
     *    Db.insert(Row.of("user").set("name", "james"));
     *    new User().name("james").insert();
     *
     * 链式操作获取自增 id：
     *    int id = new User().name("james").insert().getId();
     * </pre>
     */
    public R insert(R row) {
        if (row.table == null) {    // 只需判断 null，table(...) 不允许传入空字符串
            throw new IllegalArgumentException("Table name can not be null.");
        }
        return config.insertExecutor.execute(this, row);
    }

    /**
     * 使用 sql 插入数据
     *
     * <pre>
     * 例子：
     *    String sql = "insert into user(id, name) values(?, ?)";
     *    Db.sql(sql, 1, "james").insert();
     *
     * 在 Model 中使用时，意义在于 Model 绑定了数据源，在操作非主数据源时可以省去指定 configId：
     *    String sql = "insert into user(id, name) values(?, ?)";
     *    User.sql(sql, 1, "james").insert();
     *
     *    注意：Model 中使用 insert() 时上无法限定 table 参数
     * </pre>
     */
    public int insert() {
        return update();
    }

    /**
     * 若没有主键值则插入，否则更新。
     *
     * <pre>
     * 操作逻辑与返回值：
     *  1: 无主键值则插入，否则更新。
     *  2: 插入操作会取回自增主键的值，并存入 row 对象之中。
     *  3: 无论是插入还是更新操作，操作成功返回 row 对象，否则抛出异常。
     * </pre>
     */
    public R insertOrUpdate(R row) {
        // 获取主键值数量
        int idValueNum = 0;
        String[] primaryKey = row.primaryKey();
        for (String pk : primaryKey) {
            if (row.get(pk) != null) {
                idValueNum++;
            }
        }

        // 无主键值执行插入操作。要么成功，要么异常
        if (idValueNum == 0) {
            return insert(row);
        }

        // 有主键值执行更新操作。要么成功，要么异常
        if (idValueNum == primaryKey.length) {
            if (update(row)) {
                return row;
            } else {
                // 这里没考虑 mysql 中，被更新字段值与数据库中字段值完全一致时 update 返回 false 的情况
                throw new AifeiDbException("Failed to update row with provided primary keys");
            }
        }

        // 主键名与主键值数量不相符则抛出异常
        throw new IllegalArgumentException(
                String.format("Must provide either all %d primary key values or none; received %d.", primaryKey.length, idValueNum)
        );
    }

    /**
     * 删。
     *
     * <pre>
     * 返回值使用：
     *  1: 若 id 值所对应的数据存在，则删除一定成功，不必通过返回值判断是否删除成功。
     *  2: 若 id 值所对应的数据不确定是否存在，并且关心删除结果时，才需要关注返回值。
     *
     * 例子：
     *    Db.delete(Row.of("user").id(123));               // 默认主键 "id"
     *    Db.delete(Row.of("user", "user_id").id(123));    // 指定主键 "user_id"
     * </pre>
     */
    public boolean delete(R row) {
        if (row.table == null) {
            throw new IllegalArgumentException("Table name can not be null.");
        }
        return config.deleteExecutor.delete(this, row);
    }

    /**
     * 使用 sql 删除数据
     * <pre>
     * 例子：
     *    String sql = "delete from blog where user_id < ?";
     *    Db.sql(sql, 123).delete();
     * </pre>
     */
    public int delete() {
        return config.deleteExecutor.execute(this);
    }

    /**
     * 改。
     *
     * <pre>
     * 返回值逻辑与使用：
     *  1: 通过 row.set(...) 系列方法放入数据，insert sql 执行成功，且更新行数为 1，则返回 true。其它情况一律返回 false。
     *  2: 通过 row.set(...) 系列方法放入的数据会被更新，row.put(...) 放入的数据不会被更新。
     *
     * 例子：
     *    Db.update(Row.of("user").id(123).set("age", 18));               // 默认主键 "id"
     *    Db.update(Row.of("user", "user_id").id(123).set("age", 18));    // 指定主键 "user_id"
     * </pre>
     */
    public boolean update(R row) {
        if (row.table == null) {    // table(...) 不允许传入空字符串，只需判断 null
            throw new IllegalArgumentException("Table name can not be null.");
        }
        return config.updateExecutor.update(this, row);
    }

    /**
     * 使用 sql 更新数据
     * <pre>
     * 例子：
     *    String sql = "update account set money = ? where id = ?";
     *    Db.sql(sql, 100000000, 123).update();
     * </pre>
     */
    public int update() {
        return config.updateExecutor.execute(this);
    }

    /**
     * Enjoy sql 操作数据库
     * 1: 使用 enjoy sql + paras 操作数据库
     * 2: 通过 sql 生成 sqlId 作为 key 缓存该 enjoy sql
     * 3: 可在 sql 中指定 sqlId，从而避免生成 sqlId 来提升性能，可选功能，不指定 sqlId 则不缓存
     *
     * <pre>
     * sql 中指定 sqlId 的例子：
     *    String sql = "|user.findById| select * from user where id = ?";
     *    Db.sql(sql, 123).find();
     * 以上 sql 指定了 sqlId 值 "user.findById" 并通过分隔字符 '|' 将其与其它部分分隔开来
     *
     * 以上 sql 指定过 sqlId 的只要被执行过一次，后续需要取用的时候只需使用 sqlId 部分即可，例：
     *    String sql = "|user.findById|";    // 注意: 分隔字符 '|' 需要保留
     *    Db.sql(sql, 123).find();
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
    public D sql(String sql, Object... paras) {
        String[] idAndSql = config.idSqlFactory.splitIdAndSql(sql);
        sqlPara = (idAndSql[0] != null
                ? config.sqlKit.getSqlPara(idAndSql[0], idAndSql[1], paras)
                : config.sqlKit.getSqlPara(idAndSql[1], paras));
        // 当 sql 为原生 sql 时，传入原参数匹配其问号占位字符
        sqlPara.setParasIfNotEnjoySql(paras);
        return (D) this;
    }

    /**
     * Enjoy sql 操作数据库
     * 1: 使用 enjoy sql 操作数据库
     * 2: 通过 sql 生成 sqlId 作为 key 缓存该 enjoy sql
     * 3: 可在 sql 中指定 sqlId，从而避免生成 sqlId 来提升性能，可选功能，不指定 sqlId 则不缓存
     *
     * <pre>
     * sql 中指定 sqlId 的例子：
     *    String sql = "|user.findJames| select * from user where name = 'james'";
     *    Db.sql(sql).find();
     * 以上 sql 指定了 sqlId 值 "user.findJames" 并通过分隔字符 '|' 将其与其它部分分隔开来
     *
     * 以上 sql 指定过 sqlId 的只要被执行过一次，后续需要取用的时候只需使用 sqlId 部分即可，例：
     *    String sql = "|user.findJames|";    // 注意: 分隔字符 '|' 需要保留
     *    Db.sql(sql).find();
     *
     * 分隔字符前后允许存在一个或多个空格，例如：
     *    "| user.findById | select * from user where id = 123"
     *
     * 此外，还可以通过配置 DbConfig.idSqlFactory 来定制 sqlId 分割策略
     * </pre>
     *
     * @param sql enjoy sql 或原生 sql
     */
    public D sql(String sql) {
        return sql(sql, DbKit.EMPTY_OBJECT_ARRAY);
    }

    /**
     * Enjoy sql 操作数据库
     * 1: 使用 enjoy sql + data 操作数据库
     * 2: 通过 sql 生成 sqlId 作为 key 缓存该 enjoy sql
     * 3: 可在 sql 中指定 sqlId，从而避免生成 sqlId 来提升性能，可选功能，不指定 sqlId 则不缓存
     *
     * <pre>
     * sql 中指定 sqlId 的例子：
     *    String sql = "|user.findById| select * from user where id = ?";
     *    Db.sql(sql, 123).find();
     * 以上 sql 指定了 sqlId 值 "user.findById" 并通过分隔字符 '|' 将其与其它部分分隔开来
     *
     * 以上 sql 指定过 sqlId 的只要被执行过一次，后续需要取用的时候只需使用 sqlId 部分即可，例：
     *    String sql = "|user.findById|";    // 注意: 分隔字符 '|' 需要保留
     *    Db.sql(sql, 123).find();
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
    public D sql(String sql, Map<?, ?> data) {
        String[] idAndSql = config.idSqlFactory.splitIdAndSql(sql);
        sqlPara = (idAndSql[0] != null
                ? config.sqlKit.getSqlPara(idAndSql[0], idAndSql[1], data)
                : config.sqlKit.getSqlPara(idAndSql[1], data));
        return (D) this;
    }

    /**
     * 通过 sqlId 获取外部文件中的 sql，随后的链式调用操作数据库
     * <p>
     * 注意：通过外部文件添加的 sql 需要使用 #sql(...) 指令指定 sqlId
     *
     * @param sqlId 作为 key 获取外部文件中的 sql
     * @param paras #para(int) 指令与问号占位字符所对应的参数
     */
    public D sqlById(String sqlId, Object... paras) {
        sqlPara = config.sqlKit.getSqlParaById(sqlId, paras);
        // 当 sql 为原生 sql 时，传入原参数匹配其问号占位字符
        sqlPara.setParasIfNotEnjoySql(paras);
        return (D) this;
    }

    /**
     * 通过 sqlId 获取外部文件中的 sql，随后的链式调用操作数据库
     * <p>
     * 注意：通过外部文件添加的 sql 需要使用 #sql(...) 指令指定 sqlId
     *
     * @param sqlId 作为 key 获取外部文件中的 sql
     */
    public D sqlById(String sqlId) {
        return sqlById(sqlId, DbKit.EMPTY_OBJECT_ARRAY);
    }

    /**
     * 通过 sqlId 获取外部文件中的 sql，随后的链式调用操作数据库
     * <p>
     * 注意：通过外部文件添加的 sql 需要使用 #sql(...) 指令指定 sqlId
     *
     * @param sqlId 作为 key 获取外部文件中的 sql
     * @param data #para(name) 与 #(name) 等 enjoy 指令使用的数据
     */
    public D sqlById(String sqlId, Map<?, ?> data) {
        sqlPara = config.sqlKit.getSqlParaById(sqlId, data);
        return (D) this;
    }

    /**
     * 通过纯 sql + para 操作数据库，不使用 enjoy sql
     */
    public D sqlPara(String sql, Object... paras) {
        sqlPara = new SqlPara().setSql(sql).setParaArray(paras);
        return (D) this;
    }

    /**
     * 通过纯 sql + para 操作数据库，不使用 enjoy sql
     */
    public D sqlPara(SqlPara sqlPara) {
        this.sqlPara = sqlPara;
        return (D) this;
    }

    public SqlPara sqlPara() {
        return sqlPara;
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
    public D select(String fields) {
        if (fields == null) {
            throw new IllegalArgumentException("Select fields can not be null.");
        }
        this.select = fields;   // fields.trim();
        return (D) this;
    }

    public String select() {
        return select != null ? select : "*";
    }

    /**
     * 通过 sql(...)、sqlById(...) 方法中指定的 sql 与参数进行查询。
     * <p>
     * 后续不带 String table 参数的 find/query 系列方法多数基于 sql(...)、
     * sqlById(...) 方法中指定的 sql 与参数进行查询，不再重复说明
     */
    public List<R> find() {
        return config.findExecutor.execute(this, null);
    }

    /**
     * 查询第一条数据
     */
    public R findFirst() {
        return config.findExecutor.findFirst(this);
    }

    /**
     * 查询一条数据。查询结果数量必须为 1，否则抛出异常
     * <p>
     * 备注：本方法相对于 findFirst 强化了在 sql 中使用数量限定子句，例如 mysql 的 limit 1
     */
    public R findOne() {
        return config.findExecutor.findOne(this, false, null);
    }

    /**
     * 查询一条数据。
     * 查询结果数量必须为 1，否则调用 exceptionMessageFun 函数，并用其返回值作为 message 创建异常并抛出
     *
     * <p>
     * 备注：本方法相对于 findFirst 强化了在 sql 中使用数量限定子句，例如 mysql 的 limit 1
     * <pre>
     * 例子：
     *    Db.sql("select * from orders where id = ?", id).findOne(n -> "订单数必须为 1，不能为 " + n);
     * <pre>
     */
    public R findOne(Function<Integer, String> exceptionMessageFun) {
        return config.findExecutor.findOne(this, false, exceptionMessageFun);
    }

    // /**
    //  * 查询一条数据。查询结果数量必须为 1，否则使用 exceptionMessage 参数抛出异常
    //  * <p>
    //  * 例子：
    //  *   Db.sql("select * from orders where id = ?", id).findOne("订单的数量必须为 1，订单 id :" + id);
    //  */
    // public R findOne(String exceptionMessage) {
    //     return config.findExecutor.findOne(this, false, exceptionMessage);
    // }

    /**
     * 查询一条数据。查询结果数量可以为 1 或者 0，否则抛出异常
     * <p>
     * 备注：本方法相对于 findFirst 强化了在 sql 中使用数量限定子句，例如 mysql 的 limit 1
     */
    public R findOneOrNull() {
        return config.findExecutor.findOne(this, true, null);
    }

    /**
     * 查询一条数据。
     * 查询结果数量可以为 1 或者 0，否则调用 exceptionMessageFun 函数，并用其返回值作为 message 创建异常并抛出
     *
     * <p>
     * 备注：本方法相对于 findFirst 强化了在 sql 中使用数量限定子句，例如 mysql 的 limit 1
     */
    public R findOneOrNull(Function<Integer, String> exceptionMessageFun) {
        return config.findExecutor.findOne(this, true, exceptionMessageFun);
    }

    // /**
    //  * 查询一条数据。查询结果数量可以为 1 或者 0，否则使用 exceptionMessage 参数抛出异常
    //  */
    // public R findOneOrNull(String exceptionMessage) {
    //     return config.findExecutor.findOne(this, true, exceptionMessage);
    // }

    /**
     * 查询是否存在数据，有则返回 true，否则返回 false。
     * 如果查询语句可能返回多条数据，建议限定数据返回数量提升性能，例如 mysql 下使用 limit 1
     * <p>
     * 注意：不要添加 queryExists，因为 QueryExecutor 不支持使用回调，无法在得到一条数据时就返回
     *      性能不如 FindExecutor
     */
    public boolean findExists() {
        return config.findExecutor.findExists(this);
    }

    /**
     * 用于表示分页 sql 是否含有 group by。
     * <p>
     * 注意：嵌套 sql 仅指最外层是否含有 group by
     */
    public D hasGroupBy(boolean hasGroupBy) {
        this.hasGroupBy = hasGroupBy;
        return (D) this;
    }

    /**
     * 分页
     *
     * <pre>
     * 例子：
     *    Page<Row> = Db.sql("select * from article").paginate(1, 10);
     *    Page<User> = User.sql("select * from user").paginate(1, 10);
     * </pre>
     */
    public Page<R> paginate(int pageNum, int pageSize) {
        return config.paginateExecutor.execute(this, pageNum, pageSize, hasGroupBy, null);
    }

    /**
     * 分页
     * <p>
     * TotalRows 参数用于定制 totalRows 的获取方式，主要两个应用场景：
     * 1: 用于缓存 totalRows 提升性能，多数情况下 totalRows 无需每次都查询最新的值
     * 2: 解决复杂 order by 子句无法被正确移除的情况
     *
     * <pre>
     * 缓存 totalRows 优化性能例子：
     *    Db.sql(...).paginate(1, 10, (sqlPara, totalRowsQuery) -> {
     *       String key = sqlPara.generateKey("totalRows:");
     *       Long totalRows = Redis.use().get(key);
     *       if (totalRows == null) {
     *          totalRows = totalRowsQuery.execute();
     *          Redis.use().setex(key, 300, totalRows);
     *        }
     *        return totalRows;
     *    });
     *
     * 解决复杂 order by 子句无法被正确移除的例子：
     *    Page<Row> page = Db.sql("select * from user order by .....").paginate(1, 50, (sqlPara, totalRowsQuery) -> {
     *        // 手动查询出 totalRows 值，解决 order by 清除不干净的问题
     *        return Db.sql("select count(*) from user", sqlPara.getPara()).queryLong();
     *    });
     * </pre>
     */
    public Page<R> paginate(int pageNum, int pageSize, TotalRows totalRows) {
        return config.paginateExecutor.execute(this, pageNum, pageSize, hasGroupBy, totalRows);
    }

    /**
     * 对查询结果进行遍历，而不必将结果存入 List，而且遍历过程中可通过 return false 随时终止处理。
     * 用于查询结果数据量超大，需要节省内存的场景。
     *
     * 典型应用：多表关联查询数据存入 Elasticsearch 或者 Redisearch 做搜索功能会非常方便。
     *
     * <pre>
     *  例子：
     *     Db.sql(...).forEach(row -> {
     *         System.out.println(row.getStr(...));
     *         return true;     // 此处返回 false 可终止遍历
     *     });
     * </pre>
     */
    public void forEach(Function<R, Boolean> fun) {
        config.findExecutor.execute(this, fun);
    }

    /**
     * 从第一页开始到最后一页对所有分页进行遍历。
     *
     * 典型应用：多表关联查询数据存入 Elasticsearch 或者 Redisearch 做搜索功能会非常方便，只需写好 sql 无需关心分页。
     *
     * <pre>
     * 例子：
     *    User.sql("select * from user").forEachPage(10, (Page<User> page) -> {
     *
     *        // page.getPageNum() 可获取当前页号
     *        System.out.println("pageNum = " + page.getPageNum());
     *
     *        // 访问当前页
     *        for (User user : page.getRows()) {
     *            System.out.println(user.getName());
     *        }
     *
     *        // 返回 true 继续访问下一页，返回 false 结束访问
     *        return true;
     *    });
     * </pre>
     */
    public void forEachPage(int pageSize, Function<Page<R>, Boolean> fun) {
        config.paginateExecutor.forEachPage(this, pageSize, hasGroupBy, fun);
    }

    /**
     * 从第 startPageNum 页开始到 endPageNum 页，对这些分页进行遍历
     */
    public void forEachPage(int startPageNum, int endPageNum, int pageSize, Function<Page<R>, Boolean> fun) {
        config.paginateExecutor.forEachPage(this, startPageNum, endPageNum, pageSize, hasGroupBy, fun);
    }

    // query 系方法 -----------------------------------------------------------------------------------------

    public <T> List<T> query() {
        return config.queryExecutor.execute(this, false);
    }

    /**
     * 使用 query 方法查询，但仅取第一条数据返回
     * <p>
     * 建议使用 findFirst 代替本方法，findFirst 针对返回多条数据的情况进行了优化。
     * 否则建议在 sql 中限定只返回一条数据，例如 mysql 数据库使用 limit 1
     */
    public <T> T queryFirst() {
        return config.queryExecutor.queryFirst(this);
    }

    /**
     * 查询一条数据。查询结果数量必须为 1，否则抛出异常
     */
    public <T> T queryOne() {
        return config.queryExecutor.queryOne(this, false, null);
    }

    /**
     * 查询一条数据。
     * 查询结果数量必须为 1，否则调用 exceptionMessageFun 函数，并用其返回值作为 message 创建异常并抛出
     */
    public <T> T queryOne(Function<Integer, String> exceptionMessageFun) {
        return config.queryExecutor.queryOne(this, false, exceptionMessageFun);
    }

    /**
     * 查询一条数据。查询结果数量可以为 1 或者 0，否则抛出异常
     */
    public <T> T queryOneOrNull() {
        return config.queryExecutor.queryOne(this, true, null);
    }

    /**
     * 查询一条数据。
     * 查询结果数量可以为 1 或者 0，否则调用 exceptionMessageFun 函数，并用其返回值作为 message 创建异常并抛出
     */
    public <T> T queryOneOrNull(Function<Integer, String> exceptionMessageFun) {
        return config.queryExecutor.queryOne(this, true, exceptionMessageFun);
    }

    public <T> T queryField() {
        return config.queryExecutor.queryField(this);
    }

    public <T> T queryField(T defaultValue) {
        T result = config.queryExecutor.queryField(this);
        return result != null ? result : defaultValue;
    }

    // 各类 queryXxx 方法已做类型转换
    // public <T> T queryField(Function<Object, T> converter) {
    //     Object value = queryField();
    //     return value != null ? converter.apply(value) : null;
    // }

    public String queryStr() {
        return config.queryExecutor.queryStr(this);
    }

    public Integer queryInt() {
        return config.queryExecutor.queryInt(this);
    }

    public Long queryLong() {
        return config.queryExecutor.queryLong(this);
    }

    public BigDecimal queryBigDecimal() {
        return config.queryExecutor.queryBigDecimal(this);
    }

    public BigInteger queryBigInteger() {
        return config.queryExecutor.queryBigInteger(this);
    }

    public Double queryDouble() {
        return config.queryExecutor.queryDouble(this);
    }

    public Boolean queryBoolean() {
        return config.queryExecutor.queryBoolean(this);
    }

    public Date queryDate() {
        return config.queryExecutor.queryDate(this);
    }

    public LocalDateTime queryLocalDateTime() {
        return config.queryExecutor.queryLocalDateTime(this);
    }

    public LocalDate queryLocalDate() {
        return config.queryExecutor.queryLocalDate(this);
    }

    public java.sql.Timestamp queryTimestamp() {
        return config.queryExecutor.queryTimestamp(this);
    }

    public java.sql.Time queryTime() {
        return config.queryExecutor.queryTime(this);
    }

    public byte[] queryBytes() {
        return config.queryExecutor.queryBytes(this);
    }

    public Float queryFloat() {
        return config.queryExecutor.queryFloat(this);
    }

    public Number queryNumber() {
        return config.queryExecutor.queryNumber(this);
    }

    // 底层 JDBC --------------------------------------------------------------------------

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
    public <T> T call(JdbcFun<T> fun) {
        return config.funExecutor.execute(this, fun);
    }

    // 事务 --------------------------------------------------------------------------

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
    public <T> T transaction(Atom<T> atom) {
        return config.transactionExecutor.execute(config, config.transactionIsolation, atom);
    }

    /**
     * 事务
     */
    public <T> T transaction(Isolation isolation, Atom<T> atom) {
        if (isolation == null) {
            throw new IllegalArgumentException("isolation can not be null.");
        }
        return config.transactionExecutor.execute(config, isolation, atom);
    }
}


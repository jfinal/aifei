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

/**
 * Row.
 */
public class Row extends AifeiRow<Row> {

    /**
     * 创建 Row 对象并传入表名 table，主键名默认为 "id"
     *
     * <pre>
     * 例子：
     *    // 插入
     *    Row.of("user").set("name", "James").insert();
     *
     *    // 更新
     *    Row.of("user").id(123).set("name", "James Zhan").update();
     * </pre>
     */
    public static Row of(String table) {
        return new Row().table(table);
    }

    /**
     * 创建 Row 对象并传入表名 table 与主键名 primaryKey
     */
    public static Row of(String table, String primaryKey) {
        return new Row().table(table, primaryKey);
    }

    /**
     * 创建 Row 对象并传入表名 table 以及复合主键名 primaryKey1、primaryKey2
     */
    public static Row of(String table, String primaryKey1, String primaryKey2) {
        return new Row().table(table, primaryKey1, primaryKey2);
    }

    // 不提供 create() 方法，鼓励使用 of(table) 传入 table 参数
    // public static Row create() {return new Row();}

    /**
     * 为单主键 table 设置主键值
     *
     * <pre>
     * 例子：
     *   // 更新 user id 为 123 的 title 为 "inventor"
     *   Row.of("user").id(123).set("title", "inventor").update();
     * </pre>
     */
    public Row id(Object id) {
        return put(primaryKey()[0], id);
    }

    /**
     * 获取单主键 table 的主键值
     */
    public <T> T id() {
        return get(primaryKey()[0]);
    }

    /**
     * 设置复合主键值
     *
     * <pre>
     * 例子：
     *   // 插入、删除 user 与 role 多对多关联表 user_role 的数据
     *   Row userRole = Row.of("user_role", "user_id", "role_id").compositeId(123, 456);
     *   userRole.insert();
     *   userRole.delete();
     * </pre>
     */
    public Row compositeId(Object id1, Object id2) {
        if (primaryKey == null) {
            throw new IllegalStateException("The composite id names must be assigned first, for example: Row.of(\"user_role\", \"user_id\", \"role_id\").compositeId(1, 2)");
        }
        if (primaryKey.length != 2) {
            throw new IllegalStateException("The number of composite ids must be 2");
        }
        put(primaryKey[0], id1);
        return put(primaryKey[1], id2);
    }

    /**
     * 插入
     *
     * <pre>
     * 例子：
     *    // 使用默认主键名 "id"
     *    Row.of("user").set("name", "james").insert();
     *
     *    // 指定主键名 "blog_id"
     *    Row.of("blog", "blog_id").set("title", "Aifei release ^_^").insert();
     *
     *    // 插入后获取自增主键值
     *    int userId = Row.of("user").set("name", "james").insert().id();
     * </pre>
     */
    public Row insert() {
        return Db.insert(this);
    }

    public Row insertOrUpdate() {
        return Db.insertOrUpdate(this);
    }

    /**
     * 删除
     *
     * <pre>
     * 例子：
     *    Row.of("user").id(123).delete();
     * </pre>
     */
    public boolean delete() {
        return Db.delete(this);
    }

    /**
     * 更新
     *
     * <pre>
     * 例子：
     *    Row.of("user").id(123).set("name", "james").update();
     * </pre>
     */
    public boolean update() {
        return Db.update(this);
    }
}



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
 * TotalRows 用于为分页查询提供总行数 totalRows 数据
 *
 * <pre>
 * 1：实现缓存 totalRows，减少一次数据库查询
 *
 *    // 使用 Redis 的 get、setex 方法做缓存
 *    Db.sql("select * from blog").paginate(1, 10, (sqlPara, totalRowsQuery) -> {
 *        String key = sqlPara.generateKey("totalRows:");
 *        Long totalRows = Redis.use().get(key);        // 取 redis 缓存
 *        if (totalRows == null) {
 *            totalRows = totalRowsQuery.execute();     // 调用默认 totalRows 查询
 *            Redis.use().setex(key, 300, totalRows);   // 存 redis 缓存
 *        }
 *        return totalRows;
 *    });
 *
 * 2：解决分页查询 totalRows 数据，复杂嵌套的 order by 子句去除干净，从而无法查询 totalRows 的问题
 *    注意：jfinal 中通过 paginateByFullSql(...) 来解决该问题，但并不优雅
 *
 *    // 以下 sql 中的 order by 子句为嵌套子查询，其中的 order by 子句无法去除干净，从而无法查到 totalRows
 *    String sql = "select * from blog where user_id = ? order by (select ... from (select ...) )"；
 *    Db.sql(sql, 123)
 *      .paginate(1, 10, (sqlPara, totalRowsQuery) -> {
 *        // 手写查询 totalRows 的 sql，解决 order by 子句去除不干净的问题
 *        return Db.sql("select count(*) from blog where user_id = ?", sqlPara.getParaArray()).queryLong();
 *    });
 *
 * </pre>
 */
@FunctionalInterface
public interface TotalRows {
    long get(SqlPara sqlPara, TotalRowsQuery totalRowsQuery);
}




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
 * TotalRowsQuery 用于分页查询中延迟查询总行数（totalRows）的逻辑封装。
 *
 * <pre>
 * 该接口仅作为 TotalRows.get(SqlPara, TotalRowsQuery) 方法的第二个参数使用，
 * 由框架自动提供其默认实现，内部封装了用于获取总行数的 SQL 查询逻辑，并通过闭包方式
 * 延迟执行（即调用 execute() 方法时才真正执行查询）
 *
 * 当用户希望自定义 totalRows 的获取方式（如 Redis 缓存、手写 SQL 等）时，可选择
 * 是否调用该默认逻辑。此机制可实现 totalRows 缓存、复杂 SQL 场景下的查询替代等高级
 * 功能。
 *
 * 示例用法：
 *  Db.sql("select * from blog").paginate(1, 10, (sqlPara, totalRowsQuery) -> {
 *      String key = sqlPara.generateKey("totalRows:");
 *      Long totalRows = Redis.use().get(key);
 *      if (totalRows == null) {
 *          totalRows = totalRowsQuery.execute();       // 延迟调用默认 totalRows 查询
 *          Redis.use().setex(key, 300, totalRows);
 *      }
 *      return totalRows;
 *  });
 * </pre>
 */
@FunctionalInterface
public interface TotalRowsQuery {

    /**
     * 执行默认的 totalRows 查询逻辑，该方法由框架自动实现，仅在用户未提供自定义逻辑时才需调用。
     *
     * @return 分页查询的总记录数
     */
    long execute();
}



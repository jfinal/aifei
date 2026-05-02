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

package cn.aifei.db.hook;

import cn.aifei.db.core.AifeiDao;
import cn.aifei.db.core.AifeiRow;
import cn.aifei.db.core.Page;
import cn.aifei.db.core.SqlPara;

/**
 * PaginateHook
 */
public interface PaginateHook {

    /**
     * PaginateExecutor 执行 queryTotalRows sql 之前回调，返回值将传递给 afterQueryTotalRows
     */
    Object beforeQueryTotalRows(AifeiDao<?, ?> dao, Boolean hasGroupBy, SqlPara queryTotalRows);

    /**
     * PaginateExecutor 执行 queryTotalRows sql 成功之后回调，fromBeforeQueryTotalRows 参数来自 beforeQueryTotalRows 返回值
     */
    void afterQueryTotalRows(AifeiDao<?, ?> dao, long ret, Object fromBeforeQueryTotalRows);

    /**
     * PaginateExecutor 执行分页 sql 之前回调，返回值将传递给 afterPaginate
     */
    Object beforePaginate(AifeiDao<?, ?> dao, int pageNum, int pageSize, long totalRows, SqlPara paginate);

    /**
     * PaginateExecutor 执行分页 sql 成功之后回调，fromBeforePaginate 参数来自 beforePaginate 返回值
     */
    <T extends AifeiRow<T>> void afterPaginate(AifeiDao<?, T> dao, Page<T> page, Object fromBeforePaginate);
}

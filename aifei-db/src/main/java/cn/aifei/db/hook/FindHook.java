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
import java.util.List;

/**
 * FindHook
 *
 * <pre>
 * 应用场景举例：
 * 1：大厂检查 SQL 是否使用了 select *，强制阻止 select * 子句，提升代码规范性。建议中小企业也强制不允许使用 select *
 * 2：在 beforeFind 中得到时间戳并返回，在 afterFind 通过收到的时间戳得到查询耗时，从而发现并记录 "SQL 慢查询"
 * </pre>
 */
public interface FindHook {

    /**
     * FindExecutor 执行 sql 之前回调，返回值将传递给 afterFind
     */
    Object beforeFind(AifeiDao<?, ?> dao);

    /**
     * FindExecutor 执行 sql 之后回调，fromBeforeFind 参数来自 beforeFind 返回值
     */
    <T extends AifeiRow<T>> void afterFind(AifeiDao<?, T> dao, List<T> rowList, Object fromBeforeFind);
}




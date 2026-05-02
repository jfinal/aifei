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

/**
 * DeleteHook
 *
 * <pre>
 * 应用场景举例：
 * 1：检测 delete sql 是否包含 where 条件，避免误删数据
 * 2：统一透明化实现逻辑删除，业务主表不允许物理删除而是更新删除标记。row.table() 获取表名并判断哪些需逻辑删除
 * 3：数据删除之后，需要将已删数据转移到历史表
 * 4：数据删除之后，需要写日志到文件或数据库
 * 5：安全加固。例如：防范某些重要数据被删除，如超级管理员账号
 * </pre>
 */
public interface DeleteHook {

    /**
     * DeleteExecutor 执行 delete sql 之前回调，返回值将传递给 afterSqlDelete
     */
    Object beforeSqlDelete(AifeiDao<?, ?> dao);

    /**
     * DeleteExecutor 执行 delete sql 成功之后回调，fromBeforeSqlDelete 参数来自 beforeSqlDelete 返回值
     */
    void afterSqlDelete(AifeiDao<?, ?> dao, int ret, Object fromBeforeSqlDelete);

    /**
     * DeleteExecutor 删除 row 之前回调，返回值将传递给 afterRowDelete
     */
    Object beforeRowDelete(AifeiDao<?, ?> dao, AifeiRow<?> row);

    /**
     * DeleteExecutor 删除 row 成功之后回调，fromBeforeRowDelete 参数来自 beforeRowDelete 返回值
     */
    void afterRowDelete(AifeiDao<?, ?> dao, AifeiRow<?> row, Object fromBeforeRowDelete);
}




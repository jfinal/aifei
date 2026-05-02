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
 * UpdateHook
 *
 * <p>
 * 应用场景举例：
 * 1：检测 update sql 是否包含 where 条件，避免数据被破坏
 * 2：数据更新之前，对特定字段赋值。例如：updated_time
 * 3：数据更新之后，执行额外操作。例如：更新当前对象的 redis 缓存
 * 4：数据更新之后，需要做记录。例如：图书的折扣调整
 * 5：数据插入之后，触发异步流程。例如：账号更新密码后异步发送安全提醒到微信
 * 6：安全加固。例如：防范重要系统配置被更改
 */
public interface UpdateHook {

    /**
     * UpdateExecutor 执行 update sql 之前回调，返回值将传递给 afterSqlUpdate
     */
    Object beforeSqlUpdate(AifeiDao<?, ?> dao);

    /**
     * UpdateExecutor 执行 update sql 成功之后回调，fromBeforeSqlUpdate 参数来自 beforeSqlUpdate 返回值
     */
    void afterSqlUpdate(AifeiDao<?, ?> dao, int ret, Object fromBeforeSqlUpdate);

    /**
     * UpdateExecutor 更新 row 之前回调，返回值将传递给 afterRowUpdate
     */
    Object beforeRowUpdate(AifeiDao<?, ?> dao, AifeiRow<?> row);

    /**
     * UpdateExecutor 更新 row 成功之后回调，fromBeforeRowUpdate 参数来自 beforeRowUpdate 返回值
     */
    void afterRowUpdate(AifeiDao<?, ?> dao, AifeiRow<?> row, Object fromBeforeRowUpdate);
}



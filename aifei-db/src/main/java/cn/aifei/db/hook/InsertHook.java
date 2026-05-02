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
 * InsertHook
 * 注意：只处理 Row 对象的插入，不处理 sql 语句的插入。sql 语句插入的
 *      处理在 UpdateHook.beforeSqlUpdate(...) 中处理。
 *
 *
 * <pre>
 * 应用场景举例：
 * 1：数据插入之前，对特定字段赋值。例如：created_time、updated_time
 * 2：数据插入之后，执行额外操作。例如：缓存数据到 redis
 * 3：数据插入之后，触发异步流程。例如：账号创建完成后发送异步邮件到注册邮箱
 * 4：注意：如果开启了事务，且后续流程依赖事务提交后的结果，需使用事务的
 *         onCommitSuccess 回调而非这里的 afterInsert，否则事务提交之前
 *         数据库中的数据还未真正更新，如若读取会读到老数据。
 * </pre>
 */
public interface InsertHook {

    /**
     * InsertExecutor 插入 row 之前回调，返回值将传递给 afterRowInsert
     */
    Object beforeRowInsert(AifeiDao<?, ?> dao, AifeiRow<?> row);

    /**
     * InsertExecutor 插入 row 成功之后回调，fromBeforeRowInsert 参数来自 beforeRowInsert 返回值
     */
    void afterRowInsert(AifeiDao<?, ?> dao, AifeiRow<?> row, Object fromBeforeRowInsert);
}




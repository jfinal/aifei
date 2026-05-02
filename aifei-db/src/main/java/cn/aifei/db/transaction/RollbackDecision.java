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

package cn.aifei.db.transaction;

/**
 * RollbackDecision.
 *
 * <p>
 * Db.transaction(...) 返回值类型实现本接口，其返回值可决定事务是否回滚
 * 本方案比 onBeforeCommit 回调函数实现事务回滚控制要方便简洁，推荐优先使用。
 *
 * <p>
 * 在 transaction(...) 方法返回值类型没有实现本接口时，考虑使用 onBeforeCommit
 * 回调函数
 *
 * <pre>
 * 例子：
 *    public class Out implements RollbackDecision {
 *        Code code;
 *
 *        public boolean shouldRollback() {
 *            return code != Code.OK;
 *        }
 *
 *        // 其它代码省略 ...
 *    }
 *
 * 应用：
 *   // id 为 1 的账号转账 100 元到 id 为 2 的账号
 *   Db.transaction( tx -> {
 *       int money = 100;
 *       int n1 = Db.sql("update account set money = money - ? where id = ? and money >= ?", money, 1, money).update();
 *       int n2 = Db.sql("update account set money = money + ? where id = ?", money, 2).update();
 *       return n1 == 1 && n2 == 1 ? Out.ok("转账成功") : Out.fail("转账失败");
 *   });
 * </pre>
 */
public interface RollbackDecision {

    /**
     * 决定事务是否应该回滚。
     *
     * @return 如果应该回滚，返回 true；否则返回 false。
     */
    boolean shouldRollback();
}



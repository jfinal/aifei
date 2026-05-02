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
 * Atom 封装事务原子操作
 *
 * <pre>
 * 例子:
 * Db.transaction(tx -> {
 *     // 两个账户之间转账 100 元
 *     String sql = "update account set balance = balance + ? where id = ?";
 *     int money = 100;
 *
 *     int n1 = Db.sql(sql, -money, from).update();    // 余额减去 100
 *     int n2 = Db.sql(sql, money, to).update();       // 余额加上 100
 *
 *     if (n1 == 1 && n2 == 1) {
 *         return Out.ok("转账成功");              // 未调用 tx.rollback() 并且未抛出异常，自动提交事务
 *     } else {
 *         tx.rollback();                         // 调用 tx.rollback() 或者抛出异常，回滚事务
 *         return Out.fail("转账失败");
 *     }
 * });
 *
 * 注意：以上代码中，可通过 Out 类实现 RollbackDecision 接口来取代显示调用 tx.rollback() 方法实现事务回滚
 * </pre>
 */
@FunctionalInterface
public interface Atom<R> {
     R run(Transaction<R> tx) throws Exception;   // 抛出异常，避免用户 try catch 吃掉异常后导致事务无法回滚
}





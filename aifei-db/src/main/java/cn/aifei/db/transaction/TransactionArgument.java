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

import cn.aifei.argument.Argument;
import cn.aifei.argument.NoMatch;
import cn.aifei.core.Input;
import cn.aifei.core.Output;

/**
 * 结合 Transactional 拦截器为 action 方法注入 Transaction 对象，便于在拦截器场景下依然可以
 * 使用 Transaction 提供的实用功能，如 onException 回调与 rollback() 回滚
 *
 * <p>
 * 注意：Argument 机制仅为路由 action 方法解析实参，不会为普通业务层代理方法注入实参。
 *
 * <pre>
 * 例子：
 *  1: 注册 TransactionArgument
 *      settings.configArgument(kit -> {
 *          kit.register(Transaction.class, TransactionArgument.class);
 *      });
 *
 *  2: 使用 Transactional、Transaction
 *      \@Before(Transactional.class)
 *      public Vip service(Transaction<Vip> transaction) {
 *          // 使用 onException 回调，回调中不得访问数据库
 *          transaction.onException(e -> new Vip());
 *
 *          if (condition()) {
 *              // 使用 rollback() 回滚
 *              transaction.rollback();
 *              return Vip.findById(456);
 *          } else {
 *              return Vip.findById(789);
 *          }
 *      }
 * </pre>
 */
public class TransactionArgument extends Argument<Input, Output, Transaction<?>> implements NoMatch {
    @Override
    public Transaction<?> getValue(Input input, Output output) {
        Transaction<?> transaction = Transactional.getTransaction();
        if (transaction == null) {
            throw new IllegalStateException("TransactionArgument requires Transactional interceptor.");
        }
        return transaction;
    }
}

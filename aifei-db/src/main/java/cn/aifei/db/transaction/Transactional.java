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

import cn.aifei.aop.Interceptor;
import cn.aifei.aop.Invocation;
import cn.aifei.db.core.Db;

/**
 * 基于 AOP 的事务拦截器。多数情况推荐使用 Db.transaction(...) 支持事务。
 *
 * 可配合 TransactionArgument 注入 Transaction 对象，便于在拦截器场景下
 * 依然可以使用 Transaction 提供的实用功能
 *
 * <pre>
 *  注意：
 *    如果本拦截器作用的方法处在控制层则会生效，否则需要配置 ProxyFactory 才会生效，
 *    因为该配置的默配置为 NoProxyFactory，不会创建 aop 代理类。配置方法：
 *
 *       // 在配置中心的 config(Settings) 中配置
 *       public void config(Settings settings) {
 *           settings.setProxyFactory(new JavassistProxyFactory());
 *       }
 *
 *  示例：
 *       \@Before(Transactional.class)
 *       public Out transfer(Account from, Account to, BigDecimal money) {
 *         ...
 *       }
 * </pre>
 */
public class Transactional implements Interceptor {

    static final ThreadLocal<Transaction<?>> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 获取当前 Transactional 拦截器开启的事务。
     *
     * @return 当前事务，未处于 Transactional 拦截器中时返回 null
     */
    public static Transaction<?> getTransaction() {
        return THREAD_LOCAL.get();
    }

    @Override
    public void intercept(Invocation inv) throws Exception {
        Object returnValue = Db.transaction(tx -> {
            Transaction<?> previous = THREAD_LOCAL.get();
            try {
                THREAD_LOCAL.set(tx);
                inv.invoke();
                return inv.getReturnValue();    // 若返回 RollbackDecision 实例，则会参与事务回滚
            } finally {
                if (previous != null) {
                    THREAD_LOCAL.set(previous);
                } else {
                    THREAD_LOCAL.remove();
                }
            }
        });

        // action 抛出异常时，Transaction.onException(...) 的返回值需要显式写回 Invocation
        inv.setReturnValue(returnValue);
    }
}

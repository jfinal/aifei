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
 * 基于 AOP 的事务拦截器。绝大多数情况推荐使用 Db.transaction(...) 支持事务。
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

    @Override
    public void intercept(Invocation inv) throws Throwable {
        Db.transaction(tx -> proceed(inv));
    }

    private Object proceed(Invocation inv) throws Exception {
        try {
            inv.invoke();
            // 若返回 RollbackDecision 实例，则需参与事务回滚
            return inv.getReturnValue();
        } catch (Exception e) {
            throw e;
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }
}


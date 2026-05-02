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

import cn.aifei.db.core.AifeiDbException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 事务工具箱
 */
public class TransactionKit {

    private final ThreadLocal<Transaction<?>> threadLocal = new ThreadLocal<>();

    // 异常产生之后回调
    private Function<Exception, ?> onException;

    // 事务提交之前回调。该函数用于在提交前决定是否要回滚事务，以及处理返回值
    private BiConsumer<Transaction<?>, Object> onBeforeCommit;

    /**
     * 开启事务后，立即将 Transaction 对象置入 ThreadLocal
     */
    public void setTransaction(Transaction<?> transaction) {
        threadLocal.set(transaction);
    }

    /**
     * 事务开启后，ThreadLocal 中必定存在 Transaction 对象
     */
    public Transaction<?> getTransaction() {
        return threadLocal.get();
    }

    public boolean isInTransaction() {
        return threadLocal.get() != null;
    }

    public void removeTransaction() {
        threadLocal.remove();
    }

    /**
     * 关闭 ResultSet、Statement，事务未开启的前提下关闭 Connection（事务打开的 connection 在 TransactionExecutor 中关闭）
     *
     * <p>
     * 注意：虽然通常来说 Connection 在关闭时会自动 ResultSet 与 Statement，但仍必须关闭，原因不赘述
     */
    public void closeConnection(ResultSet resultSet, Statement statement, Connection connection) {
        int position = 0;
        try {
            if (resultSet != null) {resultSet.close();}

            position = 1;
            if (statement != null) {statement.close();}

            position = 2;
            // ThreadLocal 中存在 Transaction 则处在事务之中
            if (connection != null && getTransaction() == null) {
                connection.close();
            }
        } catch (Exception e) {
            // resultSet 关闭异常，不再重复关闭。执行下一步 statement 关闭
            if (position == 0) {
                if (statement != null) {try {statement.close();} catch (Exception ignored) {}}
            }

            // 若不是 connection 关闭发生异常，则需关闭它
            if (position != 2) {
                if (connection != null && getTransaction() == null) {
                    try {
                        connection.close();
                    } catch (Exception e2) {
                        throw new AifeiDbException(e2);
                    }
                }
            }
        }
    }

    /**
     * 事务未开启的前提下关闭 Connection（事务打开的 connection 在 TransactionExecutor 中关闭）
     */
    public void closeConnection(Connection connection) {
        // ThreadLocal 中存在 Transaction 则处在事务之中
        if (connection != null && getTransaction() == null) {
            try {
                connection.close();
            } catch (Exception e) {
                throw new AifeiDbException(e);
            }
        }
    }

    /**
     * 配置事务抛出异常时的默认回调函数，Transaction.onException(...) 可覆盖掉该默认回调函数
     *
     * <pre>
     * 例子：
     *     aifeiDb.getConfig().setOnTransactionException(e -> {
     *         return Out.fail("事务执行失败，请联系管理员: " + e.getMessage());
     *     });
     * <pre/>
     *
     * 可自定义类似 AppException 的业务异常或者 ReturnException 这样的业务返回异常，
     * 在其中携带具体的提示信息，进一步提升开发体验，例如：
     * <pre>
     *     aifeiDb.getConfig().setOnTransactionException(e -> {
     *         if ( e instanceof ReturnException ) {
     *             // 获取 ReturnException
     *             return Out.fail(((ReturnException) e).getReturnMessage());
     *         } else {
     *             return Out.fail("事务执行失败，请联系管理员: " + e.getMessage());
     *         }
     *     });
     * <pre/>
     */
    public void setOnException(Function<Exception, ?> onException) {
        if (this.onException != null) {
            throw new AifeiDbException("onException already set.");
        }
        this.onException = onException;
    }

    public Function<Exception, ?> getOnException() {
        return onException;
    }

    /**
     * 通常用于实现根据返回值 rollback 事务，从而免去事务中 tx.rollback() 调用。
     * 由于 RollbackDecision 接口可以更方便地实现根据返回值实现事务回滚，
     * 所以 onBeforeCommit 更常用于控制事务返回值类型等等更底层的操作
     *
     * <p>
     * 注意：不提供局部 onBeforeCommit，因为可以在事务内直接写代码，Transaction 对象
     *       内部提供局部 onBeforeCommit 无意义
     *
     * <pre>
     * 例子：
     *     aifeiDb.getConfig().setOnBeforeTransactionCommit((tx, out) -> {
     *         // 实践中可考虑限定事务返回值为相同类型，这样可以完全接管 tx.rollback()
     *         if (! (out instanceof Out) ) {
     *             // 虽然事务可返回任意类型，但如果存在根据返回值进行事务回滚的设计，建议严格限定返回值类型，以免漏掉回滚
     *             throw new AifeiDbException("事务返回值类型必须为 Out");
     *         }
     *
     *         // 以下根据返回值状态回滚功能建议通过 RollbackDecision 接口实现，更加简单方便
     *         if (((Out) out).getCode() != 0) {
     *             tx.rollback();
     *         }
     *     });
     * </pre>
     */
    public void setOnBeforeCommit(BiConsumer<Transaction<?>, Object> onBeforeCommit) {
        if (this.onBeforeCommit != null) {
            throw new AifeiDbException("onBeforeCommit already set.");
        }
        this.onBeforeCommit = onBeforeCommit;
    }

    public BiConsumer<Transaction<?>, Object> getOnBeforeCommit() {
        return onBeforeCommit;
    }
}



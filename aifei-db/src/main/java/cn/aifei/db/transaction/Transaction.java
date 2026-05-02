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

import cn.aifei.log.Log;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Transaction 封装事务
 *
 * <pre>
 * 事务设计：
 * 1: aifei db 事务采用隐式提交设计：
 *      一切正常才会提交事务，否则回滚事务。
 *
 * 2: 以下任意情况发生都将回滚事务：
 *      rollback() 被调用，rollbackIf(true) 被调用，RollbackDecision.shouldRollback() 返回 true，
 *      异常被抛出，以及其它任何 "非成功" 事件。
 *
 *      所以，禁止提供任何可供用户调用的 commit、commitIf 这类事务提交方法或者其它 commit 触发机制，
 *      即用户可控的是回滚，不存在任何回滚因素才隐式提交。
 *
 * 3: Transaction 该类内部不能 try catch 异常（onCommitSuccess 机制除外），必须将异常抛给
 *    TransactionExecutor 统一进行事务流程控制，以免扰乱事务流程
 * </pre>
 */
public class Transaction<R> {

    static final Log log = Log.get(Transaction.class);

    private final Connection connection;

    private boolean active = true;
    private boolean rollbackOnly = false;

    private int currentIsolation;
    private int originalIsolation;
    private boolean originalAutoCommit;

    // 异常产生之后回调
    private Function<Exception, R> onException;

    // 事务提交成功之后回调。不提供 onBeforeCommit 回调，提交之前的代码(包括回滚事务)可直接书写在事务之中不需要该回调
    private List<Runnable> onCommitSuccessList;

    public Transaction(Connection connection, int isolation) {
        if (connection == null) {
            throw new IllegalArgumentException("connection can not be null.");
        }
        this.connection = connection;
        this.currentIsolation = isolation;
    }

    /**
     * 获取当前事务 Connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * 开始事务
     *
     * <p>
     * 注意：开始事务 setTransactionIsolation 必须在 setAutoCommit(false) 之前调用
     */
    void begin() throws SQLException {
        // 保存 originalIsolation 用于 end() 中恢复
        originalIsolation = connection.getTransactionIsolation();

        // 比较运算符使用 != 而非 < ，用于支持隔离级别降级
        if (originalIsolation != currentIsolation) {
            connection.setTransactionIsolation(currentIsolation);
        }

        originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
    }

    /**
     * 为嵌套事务设置隔离级别
     * <p>
     * 注意：本方法仅允许嵌套事务使用
     */
    void setIsolationForNestedTransaction(int isolation) throws SQLException {
        // 比较运算符使用 < 而非 != ，嵌套事务隔离级别 "不允许" 降级
        if (this.currentIsolation < isolation) {
            this.currentIsolation = isolation;
            connection.setTransactionIsolation(isolation);
        }
    }

    /**
     * 回滚事务
     */
    public void rollback() {
        rollbackOnly = true;
    }

    /**
     * 若参数 condition 值为 true 则回滚事务
     *
     * @param condition true 值回滚事务，false 不作任何操作
     * @return 若回滚事务则返回 true，否则返回 false。（注：返回值与 condition 参数值相等）
     */
    public boolean rollbackIf(boolean condition) {
        if (condition) {
            rollbackOnly = true;
        }
        return condition;
    }

    /**
     * 判断事务是否可以提交，根据其返回的 boolean 值来决定事务的返回值。
     *
     * <pre>
     * 例子：
     *    rollbackIf(condition);
     *
     *    return tx.canCommit() ? Out.ok("成功") : Out.fail("失败");
     *
     * 注意：上例未使用 RollbackDecision 接口机制，回滚事务需要明确调用 rollback()
     *       或 rollbackIf(condition) 方法，或者抛出异常
     * </pre>
     */
    public boolean canCommit() {
        return !rollbackOnly;
    }

    /**
     * 立即提交事务。仅供框架内部使用
     */
    void commitImmediately() throws SQLException {
        if (active /* && !rollbackOnly */) {
            connection.commit();
            active = false;                 // 必须后置，提交失败仍需回滚事务

            if (onCommitSuccessList != null) {
                executeOnCommitSuccess();
            }
        }
    }

    /**
     * 立即回滚事务。仅供框架内部使用
     */
    void rollbackImmediately() throws SQLException {
        // active 避免重复回滚确保幂等性，TransactionExecutor 中有两处回滚调用
        if (active) {                       // 回滚事务无需判断 rollbackOnly
            active = false;                 // 必须前置，回滚异常时避免重复回滚
            connection.rollback();
        }
    }

    /**
     * 结束事务。
     *
     * <p>
     * 注意：结束事务 setAutoCommit 必须在 setTransactionIsolation 之前调用
     */
    void end() throws SQLException {
        // 恢复为 originalAutoCommit
        connection.setAutoCommit(originalAutoCommit);

        // 恢复为 originalIsolation
        if (originalIsolation != currentIsolation) {
            connection.setTransactionIsolation(originalIsolation);
        }
    }

    /**
     * 设置异常处理函数，函数返回值将成为 transaction(...) 的返回值。
     * 当前事务的 onException 高于全局
     */
    public void onException(Function<Exception, R> onException) {
        this.onException = onException;     // 注意：这里必须允许 null 值，框架内部复位上层函数可能为 null
    }

    Function<Exception, R> getOnException() {
        return onException;
    }

    // 辅助 TransactionExecutor 正确调用嵌套事务各层级 onException
    Function<Exception, R> getAndRemoveOnException() {
        Function<Exception, R> ret = onException;
        onException = null;
        return ret;
    }

    /**
     * 设置当前事务提交成功之后的回调函数。
     *
     * <p>
     * 典型的应用场景是事务提交成功后在其它线程中更新缓存
     * 注意：该回调在事务提交成功后才被调用，如果事务提交时抛出异常则不会被调用
     *
     * <p>
     * 警告：回调发生异常不会向外抛出，如需处理异常情况需在回调中自行 try catch
     */
    public void onCommitSuccess(Runnable onCommitSuccess) {
        if (onCommitSuccess != null) {
            if (onCommitSuccessList == null) {
                onCommitSuccessList = new ArrayList<>(3);
            }
            onCommitSuccessList.add(onCommitSuccess);
        }
    }

    /**
     * 事务提交成功之后回调 onCommitSuccess
     *
     * <p>
     * 注意，此回调不向外抛出异常
     * 1：调用方需要在 onCommitSuccess 函数中自行 try catch 捕获异常进行适当处理
     * 2：此回调发生在事务提交成功之后，抛出异常无法回滚事务
     * 3：此回调异常不向外传播，保障事务提交成功后的主线流程不受影响
     * 4：此回调通常用于在事务提交后进行异步操作，例如更新缓存、发送通知等等
     */
    private void executeOnCommitSuccess() {
        // 内层事务中的 onCommitSuccess 优先执行
        for (int i = onCommitSuccessList.size() - 1; i >= 0; i--) {
            try {
                onCommitSuccessList.get(i).run();
            } catch (Exception e) {
                log.error(e.getMessage(), e);   // 未抛出的异常做日志
            }
        }
    }
}





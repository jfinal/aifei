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

    // 当前事务使用的连接，构造后不可变
    private final Connection connection;

    // 事务生命周期状态
    private State state = State.NEW;

    // 独立于生命周期状态的回滚决策标记，只允许从 false 变为 true
    private boolean rollbackOnly = false;

    // 当前事务要求的隔离级别，嵌套事务可能提升该值
    private int currentIsolation;

    // 开始事务前的连接状态，用于 end() 恢复
    private Integer originalIsolation;
    private Boolean originalAutoCommit;

    // 异常发生后的回调
    private Function<Exception, R> onException;

    // 正在执行当前事务的 onException 回调，防止回调继续复用当前事务的连接
    private boolean executingOnException = false;

    // 事务提交成功后的回调。不提供 onBeforeCommit 回调，提交前的代码（包括回滚事务）可直接写在事务中，无需该回调
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
     *
     * <p>
     * 业务代码不得直接调用 Connection 的 commit、rollback、setAutoCommit 等事务控制方法，
     * 否则会绕过 Transaction 的状态管理。
     */
    public Connection getConnection() {
        checkOperationAllowed();
        return connection;
    }

    /**
     * 开始事务
     *
     * <p>
     * 注意：开始事务 setTransactionIsolation 必须在 setAutoCommit(false) 之前调用
     */
    void begin() throws SQLException {
        if (state != State.NEW) {
            return;
        }

        // 保存 originalIsolation 用于 end() 中恢复
        originalIsolation = connection.getTransactionIsolation();

        // 比较运算符使用 != 而非 < ，用于支持隔离级别降级
        if (originalIsolation != currentIsolation) {
            connection.setTransactionIsolation(currentIsolation);
        }

        originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        state = State.ACTIVE;
    }

    /**
     * 为嵌套事务设置隔离级别
     * <p>
     * 注意：本方法仅允许嵌套事务使用
     */
    void setIsolationForNestedTransaction(int isolation) throws SQLException {
        if (state != State.ACTIVE) {
            return;
        }

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
            rollback();
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
        return state == State.ACTIVE && !rollbackOnly;
    }

    /**
     * 立即提交事务。仅供框架内部使用
     */
    void commitImmediately() throws SQLException {
        if (state != State.ACTIVE) {
            throw new IllegalStateException("Transaction is not active: " + state);
        }
        if (rollbackOnly) {
            throw new IllegalStateException("Transaction has been marked rollback-only");
        }

        state = State.COMMITTING;
        connection.commit();
        state = State.COMMITTED;
    }

    /**
     * 立即回滚事务。仅供框架内部使用
     */
    void rollbackImmediately() throws SQLException {
        // 回滚用于异常清理，必须保持幂等：
        // NEW 尚未确认开启；ROLLING_BACK 不得重试；
        // COMMITTED、ROLLED_BACK 已完成，以上状态均静默返回。
        // COMMITTING 表示提交失败或结果未确认，仍需尝试回滚。
        if (state == State.ACTIVE || state == State.COMMITTING) {
            state = State.ROLLING_BACK;
            connection.rollback();
            state = State.ROLLED_BACK;
        }
    }

    /**
     * 结束事务。
     *
     * <p>
     * 注意：结束事务 setAutoCommit 必须在 setTransactionIsolation 之前调用
     */
    void end() throws SQLException {
        // 仅在事务尚未成功开启，或已经确认提交/回滚后恢复连接状态。
        // 未决事务中改变 autoCommit，JDBC 规定可能直接提交当前事务；
        // 未决事务中改变 transactionIsolation 的行为由驱动定义，也允许提交当前事务。
        if (state != State.NEW && state != State.COMMITTED && state != State.ROLLED_BACK) {
            return;
        }

        // 恢复为 originalAutoCommit
        if (originalAutoCommit != null) {
            connection.setAutoCommit(originalAutoCommit);
        }

        // 恢复为 originalIsolation
        if (originalIsolation != null && originalIsolation != currentIsolation) {
            connection.setTransactionIsolation(originalIsolation);
        }
    }

    /**
     * 设置异常处理函数，函数返回值将成为 transaction(...) 的返回值。
     * 当前事务的 onException 高于全局
     *
     * <p>
     * 该回调仅用于将事务异常转换为失败返回值。回调执行期间不得通过当前 Transaction
     * 或同一 DbConfig 的常规数据库入口继续复用当前事务，也不得在当前事务上下文中再次开启事务，
     * 违反该约束时框架将立即抛出异常。
     * 本限制不拦截其它 DbConfig 的操作，也不拦截直接使用原始 DataSource 获取连接
     */
    public void onException(Function<Exception, R> onException) {
        this.onException = onException;     // 注意：这里必须允许 null 值，框架内部复位上层函数可能为 null
    }

    Function<Exception, R> getOnException() {
        return onException;
    }

    void checkOperationAllowed() {
        if (executingOnException) {
            throw new AifeiDbException("Database operations and transactions are not allowed in an onException callback.");
        }
    }

    R executeOnException(Function<Exception, R> onException, Exception exception) {
        executingOnException = true;
        try {
            return onException.apply(exception);
        } finally {
            executingOnException = false;
        }
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
     * 注意：该回调在事务提交成功后才被调用，如果事务提交时抛出异常则不会被调用。
     *      该回调在连接状态恢复、连接关闭（含恢复或关闭失败的情况）与 ThreadLocal
     *      清理之后执行，回调中的数据库操作不再处于当前事务之中
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
     * 事务提交成功之后回调 onCommitSuccess。仅供框架内部使用，
     * 由 TransactionExecutor 在连接关闭（含关闭失败的情况）与 ThreadLocal 清理之后调用，
     * 避免回调中的同线程数据库操作误用已提交事务的连接
     *
     * <p>
     * 注意，此回调不向外抛出异常
     * 1：调用方需要在 onCommitSuccess 函数中自行 try catch 捕获异常进行适当处理
     * 2：此回调发生在事务提交成功之后，抛出异常无法回滚事务
     * 3：此回调异常不向外传播，保障事务提交成功后的主线流程不受影响
     * 4：此回调通常用于在事务提交后进行异步操作，例如更新缓存、发送通知等等
     */
    void executeOnCommitSuccess() {
        // 仅事务提交成功才回调，回滚事务以及异常场景不发生回调
        if (state != State.COMMITTED || onCommitSuccessList == null) {
            return;
        }

        // 回调按注册顺序的逆序执行（后注册的先执行）
        for (int i = onCommitSuccessList.size() - 1; i >= 0; i--) {
            try {
                onCommitSuccessList.get(i).run();
            } catch (Exception e) {
                log.error(e.getMessage(), e);   // 未抛出的异常做日志
            }
        }
    }

    /**
     * JDBC 事务控制状态，不包含 rollbackOnly 仅回滚决策
     *
     * <pre>
     *  1: COMMITTING 和 ROLLING_BACK 在调用 JDBC 之前设置。如果 JDBC 调用抛出异常，
     *     状态将保持不变，表示事务完成结果尚未确认。
     *
     *  2: 状态转换：
     *      NEW -> ACTIVE -> COMMITTING -> COMMITTED
     *                |            |
     *                +------------+-> ROLLING_BACK -> ROLLED_BACK
     *
     *  3: rollbackOnly 独立于上述状态，只允许从 false 变为 true。
     * </pre>
     */
    private enum State {

        /** Transaction 已创建，但 JDBC 事务尚未确认开启 */
        NEW,

        /** JDBC 事务已经确认开启 */
        ACTIVE,

        /** 正在提交；commit 抛出异常时保持该状态，随后仍可尝试回滚 */
        COMMITTING,

        /** 已确认提交成功 */
        COMMITTED,

        /** 正在回滚；rollback 抛出异常时保持该状态，end() 据此跳过连接状态恢复 */
        ROLLING_BACK,

        /** 已确认回滚成功 */
        ROLLED_BACK
    }
}

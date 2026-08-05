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

import cn.aifei.db.core.DbConfig;
import cn.aifei.db.core.AifeiDbException;
import cn.aifei.log.Log;
import java.sql.Connection;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * TransactionExecutor 事务执行器
 */
public class TransactionExecutor {

    static final Log log = Log.get(TransactionExecutor.class);

    @SuppressWarnings("unchecked")
    public <R> R execute(DbConfig config, Isolation isolation, Atom<R> atom) {
        TransactionKit transactionKit = config.getTransactionKit();
        Transaction<R> transaction = (Transaction<R>) transactionKit.getTransaction();
        BiConsumer<Transaction<?>, Object> onBeforeCommit = transactionKit.getOnBeforeCommit();

        // 嵌套事务
        if (transaction != null) {
            // 禁止在当前事务的 onException 回调中再次开启事务（仅限制同一 DbConfig 对象）
            transaction.checkOperationAllowed();
            return handleNestedTransaction(transaction, isolation, atom, onBeforeCommit);
        }

        Connection connection = null;
        boolean transactionInitialized = false;
        try {
            connection = config.getDataSource().getConnection();
            transaction = new Transaction<>(connection, isolation.level);
            transaction.begin();
            transactionKit.setTransaction(transaction);
            transactionInitialized = true;

            R ret = atom.run(transaction);
            // 若返回值类型实现了 RollbackDecision 接口，则根据其 shouldRollback() 返回值决定是否回滚事务
            if (ret instanceof RollbackDecision && ((RollbackDecision) ret).shouldRollback()) {
                transaction.rollback();
            }
            // 内层、外层调用 onBeforeCommit 校验各自的 ret 返回值
            if (onBeforeCommit != null && transaction.canCommit()) {
                onBeforeCommit.accept(transaction, ret);
            }

            if (transaction.canCommit()) {
                transaction.commitImmediately();
            } else {
                transaction.rollbackImmediately();
            }
            return ret;

        } catch (Exception e) {
            // 异常发生立即回滚事务
            if (transaction != null) {
                try {
                    transaction.rollbackImmediately();
                } catch (Exception ex) {                // 前一个 catch 中的异常优先于当前异常
                    log.error(ex.getMessage(), ex);     // 未抛出的异常做日志
                }
            }

            // 事务初始化成功才允许执行异常回调，局部回调优先级高于全局回调
            if (transactionInitialized) {
                if (transaction.getOnException() != null) {
                    log.error(e.getMessage(), e);           // 未抛出的异常做日志
                    return transaction.executeOnException(transaction.getOnException(), e);
                } else if (transactionKit.getOnException() != null) {
                    log.error(e.getMessage(), e);           // 未抛出的异常做日志
                    return transaction.executeOnException((Function<Exception, R>) transactionKit.getOnException(), e);
                }
            }

            // 没有异常回调或者事务初始化失败时向上抛出异常
            throw e instanceof RuntimeException ? (RuntimeException) e : new AifeiDbException(e);

        } catch (Error e) {
            // Error 也必须回滚事务，但不得包装或转换为业务异常
            if (transaction != null) {
                try {
                    transaction.rollbackImmediately();
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);     // 未抛出的异常做日志
                }
            }
            throw e;

        } finally {
            boolean closeOnException = true;
            try {
                // 尝试恢复连接状态，避免状态泄漏给后续使用者；回滚失败时会跳过恢复，避免意外提交
                if (transaction != null) {
                    transaction.end();
                }
                if (connection != null) {
                    closeOnException = false;   // 避免下行代码发生异常时再次调用 connection.close()
                    connection.close();         // connection 谁获取谁关闭
                }

            } catch (Exception e) {
                if (connection != null && closeOnException) {
                    try {
                        connection.close();
                    } catch (Exception ex) {
                        log.error(ex.getMessage(), ex);     // 未抛出的异常做日志
                    }
                }

                log.error(e.getMessage(), e);               // 未抛出的异常做日志

            } finally {
                transactionKit.removeTransaction();
            }

            // 连接关闭且 ThreadLocal 清理后执行，仅提交成功时生效
            if (transaction != null) {
                transaction.executeOnCommitSuccess();
            }
        }
    }

    private <R> R handleNestedTransaction(Transaction<R> transaction, Isolation isolation, Atom<R> atom, BiConsumer<Transaction<?>, Object> onBeforeCommit) {
        // 暂存并移除上层回调，避免本层误用；finally 中恢复
        Function<Exception, R> upperLevelOnException = transaction.getAndRemoveOnException();

        try {
            transaction.setIsolationForNestedTransaction(isolation.level);

            R ret = atom.run(transaction);
            // 若返回值类型实现了 RollbackDecision 接口，则根据其 shouldRollback() 返回值决定是否回滚事务
            if (ret instanceof RollbackDecision && ((RollbackDecision) ret).shouldRollback()) {
                transaction.rollback();
            }
            // 内层、外层调用 onBeforeCommit 校验各自的 ret 返回值
            if (onBeforeCommit != null && transaction.canCommit()) {
                onBeforeCommit.accept(transaction, ret);
            }
            return ret;

        } catch (Exception e) {
            transaction.rollback();

            // 嵌套事务不能吞掉异常，因此只执行回调，不使用其返回值
            if (transaction.getOnException() != null) {
                try {
                    transaction.executeOnException(transaction.getOnException(), e);
                } catch (Exception ex) {
                    // 回调抛出的 Exception 不得掩盖原始异常，记录日志后继续抛出原始异常（Error 不捕获，仍向外抛出）
                    log.error(ex.getMessage(), ex);         // 未抛出的异常做日志
                }
            }

            // 向上抛出，避免外层误判业务成功或继续执行
            throw e instanceof RuntimeException ? (RuntimeException) e : new AifeiDbException(e);

        } catch (Error e) {
            transaction.rollback();
            throw e;

        } finally {
            // 恢复上层回调（包括 null）
            transaction.onException(upperLevelOnException);
        }
    }
}

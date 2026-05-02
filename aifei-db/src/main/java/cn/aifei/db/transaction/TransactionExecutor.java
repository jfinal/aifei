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
            return handleNestedTransaction(transaction, isolation, atom, onBeforeCommit);
        }

        Connection connection = null;
        try {
            connection = config.getDataSource().getConnection();
            transaction = new Transaction<>(connection, isolation.level);
            transactionKit.setTransaction(transaction);
            transaction.begin();

            R ret = atom.run(transaction);
            // 若返回值类型实现了 RollbackDecision 接口，则根据其 shouldRollback() 返回值决定是否回滚事务
            if (ret instanceof RollbackDecision && ((RollbackDecision) ret).shouldRollback()) {
                transaction.rollback();
            }
            // 内层、外层调用 onBeforeCommit 处理各自的 ret 返回值
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

            // 异常回调，局部回调优先级高于全局回调
            if (transaction != null && transaction.getOnException() != null) {
                log.error(e.getMessage(), e);           // 未抛出的异常做日志
                return transaction.getOnException().apply(e);
            } else if (transactionKit.getOnException() != null) {
                log.error(e.getMessage(), e);           // 未抛出的异常做日志
                return (R) transactionKit.getOnException().apply(e);
            }

            // 没有异常回调时向上抛出异常
            throw e instanceof RuntimeException ? (RuntimeException) e : new AifeiDbException(e);

        } finally {
            boolean closeOnException = true;
            try {
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
        }
    }

    private <R> R handleNestedTransaction(Transaction<R> transaction, Isolation isolation, Atom<R> atom, BiConsumer<Transaction<?>, Object> onBeforeCommit) {
        // 持有并删除上层回调函数：1、供后续在 finally 中恢复。2、避免本层无回调函数且异常发生时调用到上层回调函数
        Function<Exception, R> upperLevelOnException = transaction.getAndRemoveOnException();

        try {
            transaction.setIsolationForNestedTransaction(isolation.level);

            R ret = atom.run(transaction);
            // 若返回值类型实现了 RollbackDecision 接口，则根据其 shouldRollback() 返回值决定是否回滚事务
            if (ret instanceof RollbackDecision && ((RollbackDecision) ret).shouldRollback()) {
                transaction.rollback();
            }
            // 内层、外层调用 onBeforeCommit 处理各自的 ret 返回值
            if (onBeforeCommit != null && transaction.canCommit()) {
                onBeforeCommit.accept(transaction, ret);
            }
            return ret;

        } catch (Exception e) {
            transaction.rollback();

            // 此处不 return 回调函数的执行结果，否则上层事务感知不到异常可能会认为业务执行成功（虽然事务会被回滚）
            // 由于后续仍然抛出异常，所以回调函数返回值无意义，但回调函数中可能有代码需要被执行，不必节省这点消耗
            if (transaction.getOnException() != null) {
                transaction.getOnException().apply(e);  // 注意不要 return，需在后面抛出异常
            }

            // 向上抛出：1、否则上层可能误以为事务已提交业务已成功。2、避免执行后续未执行的代码。3、上层做日志。
            throw e instanceof RuntimeException ? (RuntimeException) e : new AifeiDbException(e);

        } finally {
            // 恢复上一层异常回调函数（即使为 null），确保回调函数执行结果返回给正确的调用者（层级正确）
            transaction.onException(upperLevelOnException);
        }
    }
}




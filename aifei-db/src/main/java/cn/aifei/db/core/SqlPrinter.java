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

package cn.aifei.db.core;

import cn.aifei.db.ext.SqlFormatter;
import cn.aifei.log.Log;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * SqlPrinter 打印 sql 到控制台，提升开发体验
 *
 * <pre>
 *  高级用法：
 *
 *     // 记录执行耗时，找到 "慢 SQL" 便于优化性能
 *     public class SlowSqlPrinter extends SqlPrinter {
 *         public void print(SqlPara sqlPara) {
 *             long timeSpent = System.currentTimeMillis() - sqlPara.getTimingStartTime();
 *             System.out.println("将 timeSpent 异步写入数据库或缓存，便于找到慢 SQL: " + timeSpent);
 *
 *             // 如果仍需要打印 sql，调用父类 print
 *             super.print(sqlPara);
 *         }
 *     }
 * </pre>
 */
public class SqlPrinter {

    static final Log log = Log.get(SqlPrinter.class);

    protected boolean printSql = false;
    protected boolean printSqlToLog = false;
    protected boolean formatSql = false;
    protected Function<String, String> sqlFormatter = new SqlFormatter();

    /**
     * 配置是否开启 sql 打印功能，以及是否打印 sql 到日志
     */
    public SqlPrinter setPrintSql(boolean printSql, boolean printSqlToLog) {
        this.printSql = printSql;
        this.printSqlToLog = printSqlToLog;
        return this;
    }

    /**
     * 配置是否开启 sql 打印功能
     */
    public SqlPrinter setPrintSql(boolean printSql) {
        this.printSql = printSql;
        return this;
    }

    /**
     * 是否开启了 sql 打印
     */
    public boolean isPrintSql() {
        return printSql;
    }

    /**
     * 配置是否打印 sql 到日志
     */
    public SqlPrinter setPrintSqlToLog(boolean printSqlToLog) {
        this.printSqlToLog = printSqlToLog;
        return this;
    }

    /**
     * 是否打印 sql 到日志
     */
    public boolean isPrintSqlToLog() {
        return printSqlToLog;
    }

    /**
     * 配置是否格式化被打印的 sql，默认值为 false。开发环境配置为 true 提升开发体验。
     */
    public SqlPrinter setFormatSql(boolean formatSql) {
        this.formatSql = formatSql;
        return this;
    }

    /**
     * 配置 sql 格式化函数
     */
    public SqlPrinter setSqlFormatter(Function<String, String> sqlFormatter) {
        Objects.requireNonNull(sqlFormatter, "sqlFormatter can not be null.");
        this.sqlFormatter = sqlFormatter;
        return this;
    }

    /**
     * 开始对当前 sql 执行计时，供 print(...) 输出耗时
     */
    public void startTiming(SqlPara sqlPara) {
        if (printSql && sqlPara != null) {
            sqlPara.timingStartTime = System.currentTimeMillis();
        }
    }

    public void print(SqlPara sqlPara) {
        if (printSql && sqlPara != null) {
            if (printSqlToLog) {
                if (log.isInfoEnabled()) {
                    log.info(buildPrintInfo(sqlPara));
                }
            } else {
                System.out.print(buildPrintInfo(sqlPara));
            }
        }
    }

    protected String buildPrintInfo(SqlPara sqlPara) {
        String sql = formatSql ? sqlFormatter.apply(sqlPara.getSql()) : sqlPara.getSql();
        List<Object> paraList = sqlPara.getPara();
        int paraCount = paraList != null ? paraList.size() : 0;

        StringBuilder sb = new StringBuilder(sql.length() + paraCount * 16 + 50);
        sb.append("\nSQL: ").append(sql).append("\nPARA: [");
        for (int i = 0; i < paraCount; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(paraList.get(i));
        }
        sb.append("]\n");

        if (sqlPara.timingStartTime > 0) {
            long timeSpent = System.currentTimeMillis() - sqlPara.timingStartTime;
            sb.append("TIME: ").append(timeSpent).append(" ms\n");
        }

        return sb.toString();
    }
}



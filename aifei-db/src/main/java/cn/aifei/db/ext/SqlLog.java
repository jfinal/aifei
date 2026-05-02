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

package cn.aifei.db.ext;

import cn.aifei.db.core.SqlPara;
import cn.aifei.db.core.SqlPrinter;
import cn.aifei.log.Log;
import java.util.List;

/**
 * SqlLog 将 sql 打印到日志
 *
 * <pre>
 *  配置方法一：
 *    aifeiDb.config(config -> {
 *       config.setSqlPrinter(new SqlLog());
 *    });
 *
 * 配置方法二：
 *    aifeiDb.getConfig().setSqlPrinter(new SqlLog());
 * </pre>
 */
public class SqlLog extends SqlPrinter {

    static final Log log = Log.get(SqlLog.class);

    @Override
    public void print(SqlPara sqlPara) {
        if (!printSql || !log.isInfoEnabled()) {
            return;
        }

        String sql = formatSql ? sqlFormatter.apply(sqlPara.getSql()) : sqlPara.getSql();
        List<Object> paraList = sqlPara.getPara();
        int paraCount = paraList != null ? paraList.size() : 0;

        StringBuilder sb = new StringBuilder(sql.length() + paraCount * 16 + 32);
        sb.append("SQL: ").append(sql).append("\nPARA: [");
        for (int i = 0; i < paraCount; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(paraList.get(i));
        }
        sb.append("]");

        log.info(sb.toString());
    }
}



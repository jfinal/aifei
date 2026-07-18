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

package cn.aifei.db.dialect;

import cn.aifei.db.core.SqlPara;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SqlServerDialect
 *
 * // language=TSQL
 */
public class SqlServerDialect extends Dialect {

    // findSql.replaceFirst("(?i)select", "")
    private static final Pattern SELECT_PATTERN = Pattern.compile("select", Pattern.CASE_INSENSITIVE);

    @Override
    public char quoteLeft() {
        return '[';
    }

    @Override
    public char quoteRight() {
        return ']';
    }

    /**
     * Microsoft JDBC Driver 4.2+ 将 java.util.Date 明确映射为 JDBC TIMESTAMP。
     */
    @Override
    public void fillStatement(PreparedStatement pst, List<?> paras) throws SQLException {
        if (paras != null) {
            for (int i = 0, size = paras.size(); i < size; i++) {
                pst.setObject(i + 1, paras.get(i));
            }
        }
    }

    @Override
    public SqlPara paginate(int pageNum, int pageSize, SqlPara sqlPara) {
        int end = pageNum * pageSize;
        if (end <= 0) {
            end = pageSize;
        }
        int begin = (pageNum - 1) * pageSize;
        if (begin < 0) {
            begin = 0;
        }

        String findSql = sqlPara.getSql();
        StringBuilder ret = new StringBuilder(findSql.length() + 180);
        ret.append("SELECT * FROM ( SELECT row_number() OVER (ORDER BY tempcolumn) temprownumber, * FROM ");
        ret.append(" ( SELECT TOP ").append(end).append(" tempcolumn=0,");
        ret.append(SELECT_PATTERN.matcher(findSql).replaceFirst(""));
        ret.append(")vip)mvp WHERE temprownumber>").append(begin);
        return new SqlPara(ret.toString(), sqlPara.getPara());
    }
}


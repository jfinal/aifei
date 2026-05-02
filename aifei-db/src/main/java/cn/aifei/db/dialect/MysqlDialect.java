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

/**
 * MysqlDialect
 *
 * // language=MySQL
 */
public class MysqlDialect extends Dialect {

    @Override
    public char quoteLeft() {
        return '`';
    }

    @Override
    public char quoteRight() {
        return '`';
    }

    /**
     * 仅 Mysql 覆盖父类 fillStatement，未处理日期类型
     */
    @Override
    public void fillStatement(PreparedStatement pst, List<Object> paras) throws SQLException {
        if (paras != null) {
            for (int i = 0, size = paras.size(); i < size; i++) {
                pst.setObject(i + 1, paras.get(i));
            }
        }
    }

    @Override
    public SqlPara paginate(int pageNum, int pageSize, SqlPara sqlPara) {
        // 当前页在 SQL 查询中的起始行号
        int offset = (pageNum - 1) * pageSize;
        String findSql = sqlPara.getSql();
        StringBuilder ret = new StringBuilder(findSql.length() + 30);
        ret.append(findSql).append(" LIMIT ").append(offset).append(", ").append(pageSize);
        return new SqlPara(ret.toString(), sqlPara.getPara());
    }
}





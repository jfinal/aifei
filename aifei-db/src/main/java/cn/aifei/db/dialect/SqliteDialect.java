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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * SqliteDialect.
 */
public class SqliteDialect extends Dialect {

	@Override
	public char quoteLeft() {
		return '"';
	}

	@Override
	public char quoteRight() {
		return '"';
	}

	/**
	 * Xerial SQLite JDBC 的 setObject(...) 直接支持 java.util.Date，
	 * 并使用驱动配置的日期存储格式。
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
		int offset = (pageNum - 1) * pageSize;
		String findSql = sqlPara.getSql();
		StringBuilder ret = new StringBuilder(findSql.length() + 30);
		ret.append(findSql).append(" LIMIT ").append(offset).append(", ").append(pageSize);
		return new SqlPara(ret.toString(), sqlPara.getPara());
	}

	/**
	 * SQLite 没有独立的布尔存储类型，Xerial JDBC 的 getObject(int) 会根据
	 * 实际 storage class 将 BOOLEAN 返回为 Integer，这里统一转换成 Boolean。
	 * DATETIME、TIMESTAMP 交由 Dialect 默认实现通过 getTimestamp(int) 读取。
	 */
	@Override
	public Object readColumnValue(ResultSet rs, int columnIndex, int jdbcType) throws SQLException {
        if (jdbcType == Types.BOOLEAN) {
            boolean bool = rs.getBoolean(columnIndex);
            return rs.wasNull() ? null : bool;
        } else {
			return super.readColumnValue(rs, columnIndex, jdbcType);
		}
    }
}


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

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * NullDataSource.
 */
public class NullDataSource implements DataSource {

	public static final NullDataSource instance = new NullDataSource();

	private final String msg = "NullDataSource: No DataSource configured. Please configure a valid DataSource for database operations.";

	public PrintWriter getLogWriter() throws SQLException {
		throw new UnsupportedOperationException(msg);
	}

	public void setLogWriter(PrintWriter out) throws SQLException {
		throw new UnsupportedOperationException(msg);
	}

	public void setLoginTimeout(int seconds) throws SQLException {
		throw new UnsupportedOperationException(msg);
	}

	public int getLoginTimeout() throws SQLException {
		throw new UnsupportedOperationException(msg);
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new UnsupportedOperationException(msg);
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException(msg);
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException(msg);
	}

	public Connection getConnection() throws SQLException {
		throw new UnsupportedOperationException(msg);
	}

	public Connection getConnection(String username, String password) throws SQLException {
		throw new UnsupportedOperationException(msg);
	}
}





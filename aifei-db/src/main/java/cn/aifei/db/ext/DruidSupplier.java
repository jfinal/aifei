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

import cn.aifei.enjoy.util.StrUtil;
import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.pool.DruidDataSource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * DruidSupplier 整合 Druid JDBC 连接池
 */
public class DruidSupplier implements Supplier<DataSource> {

    protected volatile boolean isStarted = false;
    protected DruidDataSource dataSource;

    //连接池的名称
    protected String name;

    // 基本属性 url、user、password
    protected String url;
    protected String username;
    protected String password;
    protected String publicKey;
    protected String driverClass;    // 由 "com.mysql.jdbc.Driver" 改为 null 让 druid 自动探测 driverClass 值

    // 初始连接池大小、最小空闲连接数、最大活跃连接数
    protected int initialSize = 1;
    protected int minIdle = 10;
    protected int maxActive = 32;

    // 配置获取连接等待超时的时间
    protected long maxWait = DruidDataSource.DEFAULT_MAX_WAIT;

    // 配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
    protected long timeBetweenEvictionRunsMillis = DruidDataSource.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;
    // 配置连接在池中最小生存的时间
    protected long minEvictableIdleTimeMillis = DruidDataSource.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;
    // 配置发生错误时多久重连
    protected long timeBetweenConnectErrorMillis = DruidDataSource.DEFAULT_TIME_BETWEEN_CONNECT_ERROR_MILLIS;

    /**
     * hsqldb - "select 1 from INFORMATION_SCHEMA.SYSTEM_USERS"
     * Oracle - "select 1 from dual"
     * DB2 - "select 1 from sysibm.sysdummy1"
     * mysql - "select 1"
     */
    protected String validationQuery;
    protected String connectionInitSql;
    protected String connectionProperties;
    protected boolean testWhileIdle = true;
    protected boolean testOnBorrow = false;
    protected boolean testOnReturn = false;

    // 是否打开连接泄露自动检测
    protected boolean removeAbandoned = false;
    // 连接长时间没有使用，被认为发生泄露时长
    protected long removeAbandonedTimeoutMillis = 300 * 1000;
    // 发生泄露时是否需要输出 log，建议在开启连接泄露检测时开启，方便排错
    protected boolean logAbandoned = false;

    // 是否缓存preparedStatement，即PSCache，对支持游标的数据库性能提升巨大，如 oracle、mysql 5.5 及以上版本
    // protected boolean poolPreparedStatements = false;	// oracle、mysql 5.5 及以上版本建议为 true;

    // 只要maxPoolPreparedStatementPerConnectionSize>0,poolPreparedStatements就会被自动设定为true，使用oracle时可以设定此值。
    protected int maxPoolPreparedStatementPerConnectionSize = -1;

    protected Integer defaultTransactionIsolation;
    protected Integer validationQueryTimeout;
    protected Integer timeBetweenLogStatsMillis;
    protected Boolean keepAlive;

    // 配置监控统计拦截的filters
    protected String filters;    // 监控统计："stat"    防SQL注入："wall"     组合使用： "stat,wall"
    protected List<Filter> filterList;

    // 启动时检测连接，以便在数据库连接异常时不能启动 aifei server。Fail Fast
    protected boolean checkConnectionOnStart = true;

    public DruidSupplier(String url, String username, String password) {
        this.url = url.trim();
        this.username = username;
        this.password = password;
    }

    public DruidSupplier(String url, String username, String password, String driverClass) {
        this.url = url.trim();
        this.username = username;
        this.password = password;
        this.driverClass = driverClass;
    }

    public DruidSupplier(String url, String username, String password, String driverClass, String filters) {
        this.url = url.trim();
        this.username = username;
        this.password = password;
        this.driverClass = driverClass;
        this.filters = filters;
    }

    /**
     * 根据 url 获取 validationQuery 值
     */
    private String getValidationQueryByUrl(String url) {
        if (url.startsWith("jdbc:mysql")) {
            return "select 1";
        } else if (url.startsWith("jdbc:oracle")) {
            return "select 1 from dual";
        } else if (url.startsWith("jdbc:db2")) {
            return "select 1 from sysibm.sysdummy1";
        } else if (url.startsWith("jdbc:hsqldb")) {
            return "select 1 from INFORMATION_SCHEMA.SYSTEM_USERS";
        } else if (url.startsWith("jdbc:derby")) {
            return "values (1)";
        } else {
            return "select 1";
        }
    }

    /**
     * 添加连接时的初始化sql。可以添加多次，在初次连接时使用，比如指定编码或者默认scheme等
     */
    public DruidSupplier setConnectionInitSql(String sql) {
        this.connectionInitSql = sql;
        return this;
    }

    public String getName() {
        return name;
    }

    /**
     * 连接池名称
     *
     * @param name
     */
    public DruidSupplier setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * 设置过滤器，如果要开启监控统计需要使用此方法或在构造方法中进行设置
     * <p>
     * 监控统计："stat"
     * 防SQL注入："wall"
     * 组合使用： "stat,wall"
     * </p>
     */
    public DruidSupplier setFilters(String filters) {
        this.filters = filters;
        return this;
    }

    public synchronized DruidSupplier addFilter(Filter filter) {
        if (filterList == null) {
            filterList = new ArrayList<Filter>();
        }
        filterList.add(filter);
        return this;
    }

    /**
     * 获取数据源 DataSource
     */
    @Override
    public DataSource get() {
        return isStarted ? dataSource : start().get();
    }

    public synchronized DruidSupplier start() {
        if (isStarted) {
            return this;
        }

        dataSource = new DruidDataSource();
        if (this.name != null) {
            dataSource.setName(this.name);
        }
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        if (driverClass != null) {
            dataSource.setDriverClassName(driverClass);
        }
        dataSource.setInitialSize(initialSize);
        dataSource.setMinIdle(minIdle);
        dataSource.setMaxActive(maxActive);
        dataSource.setMaxWait(maxWait);
        dataSource.setTimeBetweenConnectErrorMillis(timeBetweenConnectErrorMillis);
        dataSource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        dataSource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);

        if (validationQuery == null) {
            validationQuery = getValidationQueryByUrl(url);
        }
        dataSource.setValidationQuery(validationQuery);

        if (StrUtil.notBlank(connectionInitSql)) {
            List<String> connectionInitSqls = new ArrayList<String>();
            connectionInitSqls.add(this.connectionInitSql);
            dataSource.setConnectionInitSqls(connectionInitSqls);
        }
        dataSource.setTestWhileIdle(testWhileIdle);
        dataSource.setTestOnBorrow(testOnBorrow);
        dataSource.setTestOnReturn(testOnReturn);

        dataSource.setRemoveAbandoned(removeAbandoned);
        dataSource.setRemoveAbandonedTimeoutMillis(removeAbandonedTimeoutMillis);
        dataSource.setLogAbandoned(logAbandoned);

        //只要maxPoolPreparedStatementPerConnectionSize>0,poolPreparedStatements就会被自动设定为true，参照druid的源码
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(maxPoolPreparedStatementPerConnectionSize);

        if (defaultTransactionIsolation != null) {
            dataSource.setDefaultTransactionIsolation(defaultTransactionIsolation);
        }
        if (validationQueryTimeout != null) {
            dataSource.setValidationQueryTimeout(validationQueryTimeout);
        }
        if (timeBetweenLogStatsMillis != null) {
            dataSource.setTimeBetweenLogStatsMillis(timeBetweenLogStatsMillis);
        }
        if (keepAlive != null) {
            dataSource.setKeepAlive(keepAlive);
        }

        boolean hasSetConnectionProperties = false;
        if (StrUtil.notBlank(filters)) {
            try {
                dataSource.setFilters(filters);
                //支持加解密数据库
                if (filters.contains("config")) {
                    //判断是否设定了公钥
                    if (StrUtil.isBlank(this.publicKey)) {
                        throw new RuntimeException("Druid连接池的filter设定了config时，必须设定publicKey");
                    }
                    String decryptStr = "config.decrypt=true;config.decrypt.key=" + this.publicKey;
                    String cp = this.connectionProperties;
                    if (StrUtil.isBlank(cp)) {
                        cp = decryptStr;
                    } else {
                        cp = cp + ";" + decryptStr;
                    }
                    dataSource.setConnectionProperties(cp);
                    hasSetConnectionProperties = true;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        //确保setConnectionProperties被调用过一次
        if (!hasSetConnectionProperties && StrUtil.notBlank(this.connectionProperties)) {
            dataSource.setConnectionProperties(this.connectionProperties);
        }
        addFilterList(dataSource);

        checkConnectionOnStart();
        isStarted = true;
        return this;
    }

    /**
     * 检测连接，Fail Fast
     */
    protected void checkConnectionOnStart() {
        if (checkConnectionOnStart) {
            try (Connection conn = dataSource.getConnection()) {
                if (!conn.isValid(3)) {
                    throw new IllegalStateException("Database connection invalid!");
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to connect to database at startup", e);
            }
        }
    }

    public synchronized DruidSupplier stop() {
        if (isStarted) {
            dataSource.close();
            dataSource = null;
            isStarted = false;
        }
        return this;
    }

    private void addFilterList(DruidDataSource ds) {
        if (filterList != null) {
            List<Filter> targetList = ds.getProxyFilters();
            for (Filter add : filterList) {
                boolean found = false;
                for (Filter target : targetList) {
                    if (add.getClass().equals(target.getClass())) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    targetList.add(add);
            }
        }
    }

    public String getJdbcUrl() {
        return url;
    }

    /**
     * 支持高版本 druid 下配置 connectTimeout、socketTimeout。使用方法如下：
     * druidPlugin.getDruidDataSource().setConnectTimeout(xxx);
     * druidPlugin.getDruidDataSource().setSocketTimeout(xxx);
     */
    public DruidDataSource getDruidDataSource() {
        return dataSource;
    }

    public DruidSupplier set(int initialSize, int minIdle, int maxActive) {
        this.initialSize = initialSize;
        this.minIdle = minIdle;
        this.maxActive = maxActive;
        return this;
    }

    public DruidSupplier setDriverClass(String driverClass) {
        this.driverClass = driverClass;
        return this;
    }

    public DruidSupplier setInitialSize(int initialSize) {
        this.initialSize = initialSize;
        return this;
    }

    public DruidSupplier setMinIdle(int minIdle) {
        this.minIdle = minIdle;
        return this;
    }

    public DruidSupplier setMaxActive(int maxActive) {
        this.maxActive = maxActive;
        return this;
    }

    public DruidSupplier setMaxWait(long maxWait) {
        this.maxWait = maxWait;
        return this;
    }

    public DruidSupplier setDefaultTransactionIsolation(int defaultTransactionIsolation) {
        this.defaultTransactionIsolation = defaultTransactionIsolation;
        return this;
    }

    public DruidSupplier setValidationQueryTimeout(int validationQueryTimeout) {
        this.validationQueryTimeout = validationQueryTimeout;
        return this;
    }

    public DruidSupplier setTimeBetweenLogStatsMillis(int timeBetweenLogStatsMillis) {
        this.timeBetweenLogStatsMillis = timeBetweenLogStatsMillis;
        return this;
    }

    public DruidSupplier setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    public DruidSupplier setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
        return this;
    }

    public DruidSupplier setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
        return this;
    }

    /**
     * hsqldb - "select 1 from INFORMATION_SCHEMA.SYSTEM_USERS"
     * Oracle - "select 1 from dual"
     * DB2 - "select 1 from sysibm.sysdummy1"
     * mysql - "select 1"
     */
    public DruidSupplier setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
        return this;
    }

    public DruidSupplier setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
        return this;
    }

    public DruidSupplier setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
        return this;
    }

    public DruidSupplier setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
        return this;
    }

    public DruidSupplier setMaxPoolPreparedStatementPerConnectionSize(int maxPoolPreparedStatementPerConnectionSize) {
        this.maxPoolPreparedStatementPerConnectionSize = maxPoolPreparedStatementPerConnectionSize;
        return this;
    }

    public DruidSupplier setTimeBetweenConnectErrorMillis(long timeBetweenConnectErrorMillis) {
        this.timeBetweenConnectErrorMillis = timeBetweenConnectErrorMillis;
        return this;
    }

    public DruidSupplier setRemoveAbandoned(boolean removeAbandoned) {
        this.removeAbandoned = removeAbandoned;
        return this;
    }

    public DruidSupplier setRemoveAbandonedTimeoutMillis(long removeAbandonedTimeoutMillis) {
        this.removeAbandonedTimeoutMillis = removeAbandonedTimeoutMillis;
        return this;
    }

    public DruidSupplier setLogAbandoned(boolean logAbandoned) {
        this.logAbandoned = logAbandoned;
        return this;
    }

    public DruidSupplier setConnectionProperties(String connectionProperties) {
        this.connectionProperties = connectionProperties;
        return this;
    }

    public DruidSupplier setPublicKey(String publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    /**
     * 配置是否在启动时检测连接，以便在数据库连接异常时不能启动 aifei server。Fail Fast
     */
    public DruidSupplier setCheckConnectionOnStart(boolean checkConnectionOnStart) {
        this.checkConnectionOnStart = checkConnectionOnStart;
        return this;
    }
}

/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.r2dbc.jdbc;

import java.util.Arrays;
import java.util.List;

import io.r2dbc.spi.ConnectionFactoryOptions;

import org.springframework.boot.jdbc.DatabaseDriver;

/**
 * Provide default {@link R2dbcDataSourceProvider} implementations.
 *
 * @author Stephane Nicoll
 */
public abstract class R2dbcJdbcDriverProviders {

	/**
	 * Default {@link R2dbcDataSourceProvider} implementations.
	 */
	public static final List<R2dbcJdbcDriverProvider> DEFAULT = Arrays.asList(new MySqlR2dbcJdbcDriverProvider(),
			new PostgresqlR2dbcJdbcDriverProvider(), new SqlServerR2dbcJdbcDriverProvider());

	/**
	 * A {@link R2dbcDataSourceProvider} that supports the {@code h2} driver.
	 */
	public static class H2R2dbcJdbcDriverProvider extends AbstractR2dbcJdbcDriverProvider {

		public H2R2dbcJdbcDriverProvider() {
			super("h2", DatabaseDriver.H2.getDriverClassName());
		}

		@Override
		protected String getJdbcUrl(ConnectionFactoryOptions options) {
			String protocol = options.getRequiredValue(ConnectionFactoryOptions.PROTOCOL);
			if (protocol.equals("mem")) {
				return String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
						options.getRequiredValue(ConnectionFactoryOptions.DATABASE));
			}
			return null;
		}

	}

	/**
	 * A {@link R2dbcDataSourceProvider} that supports the {@code mysql} driver.
	 */
	public static class MySqlR2dbcJdbcDriverProvider extends AbstractR2dbcJdbcDriverProvider {

		public MySqlR2dbcJdbcDriverProvider() {
			super("mysql", DatabaseDriver.MYSQL.getDriverClassName());
		}

		@Override
		protected String getJdbcUrl(ConnectionFactoryOptions options) {
			StringBuilder url = new StringBuilder("jdbc:mysql://");
			if (options.hasOption(ConnectionFactoryOptions.HOST)) {
				url.append(options.getRequiredValue(ConnectionFactoryOptions.HOST));
				if (options.hasOption(ConnectionFactoryOptions.PORT)) {
					url.append(":").append(options.getValue(ConnectionFactoryOptions.PORT));
				}
			}
			url.append("/");
			url.append(options.getRequiredValue(ConnectionFactoryOptions.DATABASE));
			return url.toString();
		}

	}

	/**
	 * A {@link R2dbcDataSourceProvider} that supports the {@code postgres} driver.
	 */
	public static class PostgresqlR2dbcJdbcDriverProvider extends AbstractR2dbcJdbcDriverProvider {

		public PostgresqlR2dbcJdbcDriverProvider() {
			super("postgres", DatabaseDriver.POSTGRESQL.getDriverClassName());
		}

		@Override
		protected String getJdbcUrl(ConnectionFactoryOptions options) {
			StringBuilder url = new StringBuilder("jdbc:postgresql:");
			if (options.hasOption(ConnectionFactoryOptions.HOST)) {
				url.append("//").append(options.getValue(ConnectionFactoryOptions.HOST));
				if (options.hasOption(ConnectionFactoryOptions.PORT)) {
					url.append(":").append(options.getValue(ConnectionFactoryOptions.PORT));
				}
				url.append("/");
			}
			url.append(options.getRequiredValue(ConnectionFactoryOptions.DATABASE));
			return url.toString();
		}

	}

	/**
	 * A {@link R2dbcDataSourceProvider} that supports the {@code mssql} driver.
	 */
	public static class SqlServerR2dbcJdbcDriverProvider extends AbstractR2dbcJdbcDriverProvider {

		public SqlServerR2dbcJdbcDriverProvider() {
			super("mssql", DatabaseDriver.SQLSERVER.getDriverClassName());
		}

		@Override
		protected String getJdbcUrl(ConnectionFactoryOptions options) {
			StringBuilder url = new StringBuilder("jdbc:sqlserver://");
			if (options.hasOption(ConnectionFactoryOptions.HOST)) {
				url.append(options.getRequiredValue(ConnectionFactoryOptions.HOST));
				if (options.hasOption(ConnectionFactoryOptions.PORT)) {
					url.append(":").append(options.getValue(ConnectionFactoryOptions.PORT));
				}
			}
			url.append(";databaseName=").append(options.getRequiredValue(ConnectionFactoryOptions.DATABASE));
			return url.toString();
		}

	}

	protected abstract static class AbstractR2dbcJdbcDriverProvider implements R2dbcJdbcDriverProvider {

		private final String driver;

		private final String driverClassName;

		protected AbstractR2dbcJdbcDriverProvider(String driver, String driverClassName) {
			this.driver = driver;
			this.driverClassName = driverClassName;
		}

		@Override
		public String getDriverClassName() {
			return this.driverClassName;
		}

		@Override
		public String inferJdbcUrl(ConnectionFactoryOptions options) {
			String targetDriver = options.getRequiredValue(ConnectionFactoryOptions.DRIVER);
			if (targetDriver.equals(this.driver)) {
				return getJdbcUrl(options);
			}
			return null;
		}

		protected abstract String getJdbcUrl(ConnectionFactoryOptions options);

	}

}

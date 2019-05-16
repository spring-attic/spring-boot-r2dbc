/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.r2dbc;

import javax.sql.DataSource;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;

import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Builder for {@link ConnectionFactory}.
 *
 * @author Mark Paluch
 */
public final class ConnectionFactoryBuilder {

	private static final String[] CONNECTION_FACTORY_TYPE_NAMES = new String[] {
			"io.r2dbc.pool.ConnectionPool"};

	private final ConnectionFactoryOptions.Builder builder;

	public static ConnectionFactoryBuilder create(R2dbcProperties properties) {

		ConnectionFactoryOptions.Builder builder = ConnectionFactoryOptions
				.parse(properties.determineUrl()).mutate();
		String username = properties.determineUsername();
		if (StringUtils.hasText(username)) {
			builder.option(ConnectionFactoryOptions.USER, username);
		}
		String password = properties.determinePassword();
		if (StringUtils.hasText(password)) {
			builder.option(ConnectionFactoryOptions.PASSWORD, password);
		}
		String databaseName = properties.determineDatabaseName();
		if (StringUtils.hasText(databaseName)) {
			builder.option(ConnectionFactoryOptions.DATABASE, databaseName);
		}
		if (properties.getProperties() != null) {
			properties.getProperties()
					.forEach((key, value) -> builder
							.option(Option.valueOf(key), value));
		}

		return new ConnectionFactoryBuilder(builder);
	}

	public static ConnectionFactoryBuilder create() {
		return new ConnectionFactoryBuilder(ConnectionFactoryOptions.builder());
	}

	private ConnectionFactoryBuilder(ConnectionFactoryOptions.Builder builder) {
		this.builder = builder;
	}

	public ConnectionFactoryBuilder username(String username) {
		this.builder.option(ConnectionFactoryOptions.USER, username);
		return this;
	}

	public ConnectionFactoryBuilder password(String password) {
		this.builder.option(ConnectionFactoryOptions.PASSWORD, password);
		return this;
	}

	public ConnectionFactoryBuilder hostname(String host) {
		this.builder.option(ConnectionFactoryOptions.HOST, host);
		return this;
	}

	public ConnectionFactoryBuilder port(int port) {
		this.builder.option(ConnectionFactoryOptions.PORT, port);
		return this;
	}

	public ConnectionFactoryBuilder database(String database) {
		this.builder.option(ConnectionFactoryOptions.DATABASE, database);
		return this;
	}

	public ConnectionFactory build() {
		return ConnectionFactories.get(getOptions());
	}

	ConnectionFactoryOptions getOptions() {
		return this.builder.build();
	}

	@SuppressWarnings("unchecked")
	public static Class<? extends DataSource> findType(ClassLoader classLoader) {
		for (String name : CONNECTION_FACTORY_TYPE_NAMES) {
			try {
				return (Class<? extends DataSource>) ClassUtils.forName(name,
						classLoader);
			}
			catch (Exception ex) {
				// Swallow and continue
			}
		}
		return null;
	}
}

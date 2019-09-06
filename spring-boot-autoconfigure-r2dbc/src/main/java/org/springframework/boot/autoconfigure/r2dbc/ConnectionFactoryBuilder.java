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

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;

import org.springframework.util.StringUtils;

/**
 * Builder for {@link ConnectionFactory}.
 *
 * @author Mark Paluch
 * @author Tadaya Tsuyukubo
 */
public final class ConnectionFactoryBuilder {

	private final ConnectionFactoryOptions.Builder builder;

	public static ConnectionFactoryBuilder create(R2dbcProperties properties) {
		ConnectionFactoryOptions options = ConnectionFactoryOptions.parse(properties.determineUrl());
		ConnectionFactoryBuilder builder = new ConnectionFactoryBuilder(options.mutate());
		builder.applyIf(options, ConnectionFactoryOptions.USER, properties::determineUsername, Objects::nonNull);
		builder.applyIf(options, ConnectionFactoryOptions.PASSWORD, properties::determinePassword, Objects::nonNull);
		builder.applyIf(options, ConnectionFactoryOptions.DATABASE, properties::determineDatabaseName,
				StringUtils::hasText);
		if (properties.getProperties() != null) {
			properties.getProperties().forEach((key, value) -> builder.option(Option.valueOf(key), value));
		}
		return builder;
	}

	public static ConnectionFactoryBuilder create() {
		return new ConnectionFactoryBuilder(ConnectionFactoryOptions.builder());
	}

	private ConnectionFactoryBuilder(ConnectionFactoryOptions.Builder builder) {
		this.builder = builder;
	}

	private <T extends CharSequence> void applyIf(ConnectionFactoryOptions options, Option<T> option,
			Supplier<T> valueSupplier, Predicate<T> setIf) {
		if (options.hasOption(option)) {
			return;
		}
		T value = valueSupplier.get();
		if (setIf.test(value)) {
			builder.option(option, value);
		}
	}

	private void option(Option<String> option, String value) {
		this.builder.option(option, value);
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

}

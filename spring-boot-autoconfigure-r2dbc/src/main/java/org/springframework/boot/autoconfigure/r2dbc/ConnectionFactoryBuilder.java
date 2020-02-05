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

package org.springframework.boot.autoconfigure.r2dbc;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactoryOptions.Builder;
import io.r2dbc.spi.Option;

import org.springframework.util.StringUtils;

/**
 * Builder for {@link ConnectionFactory}.
 *
 * @author Mark Paluch
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 */
public final class ConnectionFactoryBuilder {

	private final ConnectionFactoryOptions.Builder optionsBuilder;

	private ConnectionFactoryBuilder(ConnectionFactoryOptions.Builder optionsBuilder) {
		this.optionsBuilder = optionsBuilder;
	}

	public static ConnectionFactoryBuilder create(R2dbcProperties properties) {
		ConnectionFactoryOptions urlOptions = ConnectionFactoryOptions.parse(properties.determineUrl());
		ConnectionFactoryBuilder builder = new ConnectionFactoryBuilder(urlOptions.mutate());
		builder.configureIf(urlOptions, ConnectionFactoryOptions.USER, properties::determineUsername, Objects::nonNull);
		builder.configureIf(urlOptions, ConnectionFactoryOptions.PASSWORD, properties::determinePassword,
				Objects::nonNull);
		builder.configureIf(urlOptions, ConnectionFactoryOptions.DATABASE, properties::determineDatabaseName,
				StringUtils::hasText);
		if (properties.getProperties() != null) {
			builder.configure((options) -> properties.getProperties()
					.forEach((key, value) -> options.option(Option.valueOf(key), value)));
		}
		return builder;
	}

	public static ConnectionFactoryBuilder create() {
		return new ConnectionFactoryBuilder(ConnectionFactoryOptions.builder());
	}

	public ConnectionFactoryBuilder configure(Consumer<Builder> options) {
		options.accept(this.optionsBuilder);
		return this;
	}

	public ConnectionFactoryBuilder hostname(String host) {
		return configure((options) -> options.option(ConnectionFactoryOptions.HOST, host));
	}

	public ConnectionFactoryBuilder port(int port) {
		return configure((options) -> options.option(ConnectionFactoryOptions.PORT, port));
	}

	public ConnectionFactoryBuilder database(String database) {
		return configure((options) -> options.option(ConnectionFactoryOptions.DATABASE, database));
	}

	public ConnectionFactoryBuilder username(String username) {
		return configure((options) -> options.option(ConnectionFactoryOptions.USER, username));
	}

	public ConnectionFactoryBuilder password(String password) {
		return configure((options) -> options.option(ConnectionFactoryOptions.PASSWORD, password));
	}

	public ConnectionFactory build() {
		return ConnectionFactories.get(buildOptions());
	}

	ConnectionFactoryOptions buildOptions() {
		return this.optionsBuilder.build();
	}

	private <T extends CharSequence> void configureIf(ConnectionFactoryOptions options, Option<T> option,
			Supplier<T> valueSupplier, Predicate<T> setIf) {
		if (options.hasOption(option)) {
			return;
		}
		T value = valueSupplier.get();
		if (setIf.test(value)) {
			this.optionsBuilder.option(option, value);
		}
	}

}

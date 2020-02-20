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

package org.springframework.boot.actuate.r2dbc;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import io.r2dbc.spi.ValidationDepth;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link HealthIndicator} to validate a R2DBC {@link ConnectionFactory}.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
public class ConnectionFactoryHealthIndicator extends AbstractReactiveHealthIndicator {

	private final ConnectionFactory connectionFactory;

	private final String validationQuery;

	/**
	 * Create a new {@link ConnectionFactoryHealthIndicator} using the specified
	 * {@link ConnectionFactory} and no validation query.
	 * @param connectionFactory the connection factory
	 * @see Connection#validate(ValidationDepth)
	 */
	public ConnectionFactoryHealthIndicator(ConnectionFactory connectionFactory) {
		this(connectionFactory, null);
	}

	/**
	 * Create a new {@link ConnectionFactoryHealthIndicator} using the specified
	 * {@link ConnectionFactory} and validation query.
	 * @param connectionFactory the connection factory
	 * @param validationQuery the validation query (can be {@code null} to use connection
	 * validation)
	 */
	public ConnectionFactoryHealthIndicator(ConnectionFactory connectionFactory, String validationQuery) {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		this.connectionFactory = connectionFactory;
		this.validationQuery = validationQuery;
	}

	@Override
	protected final Mono<Health> doHealthCheck(Health.Builder builder) {
		String product = this.connectionFactory.getMetadata().getName();
		builder.up().withDetail("database", product);
		return validate(builder).defaultIfEmpty(builder.build()).onErrorResume(Exception.class,
				(ex) -> Mono.just(builder.down(ex).build()));
	}

	protected Mono<Health> validate(Health.Builder builder) {
		if (StringUtils.hasText(this.validationQuery)) {
			return validateWithQuery(builder);
		}
		else {
			return validateWithConnectionValidation(builder);
		}
	}

	private Mono<Health> validateWithQuery(Health.Builder builder) {
		builder.withDetail("validationQuery", this.validationQuery);
		Mono<Object> result = runValidationQuery(this.connectionFactory);
		return result.map((it) -> {
			builder.withDetail("result", it);
			builder.withDetail("validationQuery", this.validationQuery);
			return builder.build();
		});
	}

	private Mono<Health> validateWithConnectionValidation(Builder builder) {
		Mono<Boolean> connectionValidation = Mono.usingWhen(this.connectionFactory.create(),
				(connection) -> Mono.from(connection.validate(ValidationDepth.REMOTE)), Connection::close,
				(o, throwable) -> o.close(), Connection::close);
		return connectionValidation.map((valid) -> {
			builder.withDetail("validationQuery", "validate(REMOTE)");
			return builder.build();
		});
	}

	private Mono<Object> runValidationQuery(ConnectionFactory connectionFactory) {
		return Mono.usingWhen(connectionFactory.create(),
				(conn) -> Flux.from(conn.createStatement(this.validationQuery).execute())
						.flatMap((it) -> it.map(this::extractResult)).next(),
				Connection::close, (o, throwable) -> o.close(), Connection::close);
	}

	private Object extractResult(Row row, RowMetadata metadata) {
		return row.get(metadata.getColumnMetadatas().iterator().next().getName());
	}

}

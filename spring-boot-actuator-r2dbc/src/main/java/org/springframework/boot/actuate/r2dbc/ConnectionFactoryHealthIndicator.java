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
import org.springframework.util.Assert;

/**
 * Health indicator for R2DBC ConnectionFactory.
 *
 * @author Mark Paluch
 */
public class ConnectionFactoryHealthIndicator extends AbstractReactiveHealthIndicator {

	private final ConnectionFactory connectionFactory;

	private final ValidationDepth validationDepth;

	private final String query;

	/**
	 * Create a new {@link ConnectionFactoryHealthIndicator} using the specified
	 * {@link ConnectionFactory}.
	 * @param connectionFactory the connection factory
	 */
	public ConnectionFactoryHealthIndicator(ConnectionFactory connectionFactory) {
		this(connectionFactory, ValidationDepth.REMOTE);
	}

	/**
	 * Create a new {@link ConnectionFactoryHealthIndicator} using the specified
	 * {@link ConnectionFactory} and validation query.
	 * @param connectionFactory the connection factory
	 * @param validationDepth the {@link ValidationDepth} to use for health checking.
	 */
	public ConnectionFactoryHealthIndicator(ConnectionFactory connectionFactory, ValidationDepth validationDepth) {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		Assert.notNull(validationDepth, "ValidationDepth must not be null");
		this.connectionFactory = connectionFactory;
		this.validationDepth = validationDepth;
		this.query = null;
	}

	/**
	 * Create a new {@link ConnectionFactoryHealthIndicator} using the specified
	 * {@link ConnectionFactory} and validation query.
	 * @param connectionFactory the connection factory
	 * @param query the validation query to use (can be {@code null})
	 */
	public ConnectionFactoryHealthIndicator(ConnectionFactory connectionFactory, String query) {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		Assert.hasText(query, "Validation query must not be empty");
		this.connectionFactory = connectionFactory;
		this.validationDepth = null;
		this.query = query;
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		String product = getProduct();
		builder.up().withDetail("database", product);
		if (this.validationDepth != null) {
			builder.withDetail("validationDepth", this.validationDepth);
			Mono<Boolean> result = validate(this.validationDepth);
			return result.map((it) -> {
				builder.withDetail("valid", it);
				return builder.build();
			}).defaultIfEmpty(builder.build()).onErrorResume(Exception.class,
					(e) -> Mono.just(builder.down(e).build()));
		}
		else {
			builder.withDetail("validationQuery", this.query);
			Mono<Object> result = runValidationQuery(this.query);
			return result.map((it) -> {
				builder.withDetail("result", it);
				return builder.build();
			}).defaultIfEmpty(builder.build()).onErrorResume(Exception.class,
					(e) -> Mono.just(builder.down(e).build()));
		}
	}

	private Mono<Boolean> validate(ValidationDepth validationDepth) {
		return Mono.usingWhen(this.connectionFactory.create(), (conn) -> Mono.from(conn.validate(validationDepth)),
				Connection::close, (o, throwable) -> o.close(), Connection::close);
	}

	private Mono<Object> runValidationQuery(String validationQuery) {
		return Mono.usingWhen(this.connectionFactory.create(),
				(conn) -> Flux.from(conn.createStatement(validationQuery).execute())
						.flatMap((it) -> it.map(this::extractResult)).next(),
				Connection::close, (o, throwable) -> o.close(), Connection::close);
	}

	private Object extractResult(Row row, RowMetadata metadata) {
		return row.get(metadata.getColumnMetadatas().iterator().next().getName());
	}

	private String getProduct() {
		return this.connectionFactory.getMetadata().getName();
	}

}

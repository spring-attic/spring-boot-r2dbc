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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Health indicator for R2DBC ConnectionFactory.
 *
 * @author Mark Paluch
 */
public class ConnectionFactoryHealthIndicator extends AbstractReactiveHealthIndicator {

	private static final String DEFAULT_QUERY = "SELECT 1";

	private final ConnectionFactory connectionFactory;

	private String query;

	/**
	 * Create a new {@link ConnectionFactoryHealthIndicator} using the specified
	 * {@link ConnectionFactory}.
	 * @param connectionFactory the connection factory
	 */
	public ConnectionFactoryHealthIndicator(ConnectionFactory connectionFactory) {
		this(connectionFactory, null);
	}

	/**
	 * Create a new {@link ConnectionFactoryHealthIndicator} using the specified
	 * {@link ConnectionFactory} and validation query.
	 * @param connectionFactory the connection factory
	 * @param query the validation query to use (can be {@code null})
	 */
	public ConnectionFactoryHealthIndicator(ConnectionFactory connectionFactory, String query) {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		this.connectionFactory = connectionFactory;
		this.query = query;
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		String product = getProduct();
		builder.up().withDetail("database", product);
		String validationQuery = getValidationQuery(product);
		if (StringUtils.hasText(validationQuery)) {
			builder.withDetail("validationQuery", validationQuery);
			Mono<Object> result = runValidationQuery(validationQuery);
			return result.map((it) -> {
				builder.withDetail("result", it);
				return builder.build();
			}).defaultIfEmpty(builder.build()).onErrorResume(Exception.class,
					(e) -> Mono.just(builder.down(e).build()));
		}
		return Mono.just(builder.build());
	}

	private Mono<Object> runValidationQuery(String validationQuery) {
		return Mono.usingWhen(this.connectionFactory.create(),
				(conn) -> Flux.from(conn.createStatement(validationQuery).execute())
						.flatMap((it) -> it.map(this::extractResult)).next(),
				Connection::close, Connection::close, Connection::close);
	}

	private Object extractResult(Row row, RowMetadata metadata) {
		return row.get(metadata.getColumnMetadatas().iterator().next().getName());
	}

	private String getProduct() {
		return this.connectionFactory.getMetadata().getName();
	}

	protected String getValidationQuery(String product) {
		String query = this.query;
		if (!StringUtils.hasText(query)) {
			DatabaseDriver specific = DatabaseDriver.fromProductName(product);
			query = specific.getValidationQuery();
		}
		if (!StringUtils.hasText(query)) {
			query = DEFAULT_QUERY;
		}
		return query;
	}

	/**
	 * Set a specific validation query to use to validate a connection. If none is set, a
	 * default validation query is used.
	 * @param query the query
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	/**
	 * Return the validation query or {@code null}.
	 * @return the query
	 */
	public String getQuery() {
		return this.query;
	}

}

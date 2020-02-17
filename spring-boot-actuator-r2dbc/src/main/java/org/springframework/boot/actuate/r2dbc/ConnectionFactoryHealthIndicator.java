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
import io.r2dbc.spi.ValidationDepth;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.util.Assert;

/**
 * {@link HealthIndicator} that validates a R2DBC {@link ConnectionFactory}.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
public class ConnectionFactoryHealthIndicator extends AbstractReactiveHealthIndicator {

	private final ConnectionFactory connectionFactory;

	/**
	 * Create a new {@link ConnectionFactoryHealthIndicator} using the specified
	 * {@link ConnectionFactory}.
	 * @param connectionFactory the connection factory
	 */
	public ConnectionFactoryHealthIndicator(ConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		this.connectionFactory = connectionFactory;
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		String product = this.connectionFactory.getMetadata().getName();
		builder.up().withDetail("database", product);
		Mono<Boolean> result = validate();
		return result.map((it) -> {
			builder.withDetail("valid", it);
			return builder.build();
		}).defaultIfEmpty(builder.build()).onErrorResume(Exception.class, (e) -> Mono.just(builder.down(e).build()));
	}

	private Mono<Boolean> validate() {
		return Mono.usingWhen(this.connectionFactory.create(),
				(conn) -> Mono.from(conn.validate(ValidationDepth.REMOTE)), Connection::close,
				(o, throwable) -> o.close(), Connection::close);
	}

}

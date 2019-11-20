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

import java.util.Random;

import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.ValidationDepth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ConnectionFactoryHealthIndicator}.
 *
 * @author Mark Paluch
 */
class ConnectionFactoryHealthIndicatorTests {

	private ConnectionFactoryHealthIndicator healthIndicator;

	private ConnectionFactory connectionFactory;

	@BeforeEach
	void init() {
		this.connectionFactory = new H2ConnectionFactory(H2ConnectionConfiguration.builder()
				.inMemory("db-" + new Random().nextInt()).option("DB_CLOSE_DELAY=-1").build());
	}

	@Test
	void healthIndicatorWithDefaultSettings() {
		this.healthIndicator = new ConnectionFactoryHealthIndicator(this.connectionFactory);
		this.healthIndicator.health().as(StepVerifier::create).assertNext((actual) -> {
			assertThat(actual.getStatus()).isEqualTo(Status.UP);
			assertThat(actual.getDetails()).containsOnly(entry("database", "H2"), entry("valid", true),
					entry("validationDepth", ValidationDepth.REMOTE));
		}).verifyComplete();
	}

	@Test
	void healthIndicatorWithCustomValidationQuery() {
		String customValidationQuery = "SELECT COUNT(*) from FOO";
		Mono.from(this.connectionFactory.create())
				.flatMapMany((it) -> Flux
						.from(it.createStatement("CREATE TABLE FOO (id INTEGER IDENTITY PRIMARY KEY)").execute())
						.flatMap(Result::getRowsUpdated).thenMany(it.close()))
				.as(StepVerifier::create).verifyComplete();
		this.healthIndicator = new ConnectionFactoryHealthIndicator(this.connectionFactory, customValidationQuery);
		this.healthIndicator.health().as(StepVerifier::create).assertNext((actual) -> {
			assertThat(actual.getStatus()).isEqualTo(Status.UP);
			assertThat(actual.getDetails()).containsOnly(entry("database", "H2"), entry("result", 0L),
					entry("validationQuery", customValidationQuery));
		}).verifyComplete();
	}

	@Test
	void healthIndicatorWithInvalidValidationQuery() {
		String invalidValidationQuery = "SELECT COUNT(*) from BAR";
		this.healthIndicator = new ConnectionFactoryHealthIndicator(this.connectionFactory, invalidValidationQuery);
		this.healthIndicator.health().as(StepVerifier::create).assertNext((actual) -> {
			assertThat(actual.getStatus()).isEqualTo(Status.DOWN);
			assertThat(actual.getDetails()).contains(entry("database", "H2"),
					entry("validationQuery", invalidValidationQuery));
			assertThat(actual.getDetails()).containsOnlyKeys("database", "error", "validationQuery");
		}).verifyComplete();
	}

}

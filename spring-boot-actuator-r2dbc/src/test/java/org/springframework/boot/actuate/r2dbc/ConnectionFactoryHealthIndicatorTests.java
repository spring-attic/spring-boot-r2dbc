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

import java.util.UUID;

import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConnectionFactoryHealthIndicator}.
 *
 * @author Mark Paluch
 */
class ConnectionFactoryHealthIndicatorTests {

	@Test
	void healthIndicatorWithDatabaseUp() {
		ConnectionFactory connectionFactory = new H2ConnectionFactory(H2ConnectionConfiguration.builder()
				.inMemory("db-" + UUID.randomUUID()).option("DB_CLOSE_DELAY=-1").build());
		ConnectionFactoryHealthIndicator healthIndicator = new ConnectionFactoryHealthIndicator(connectionFactory);
		healthIndicator.health().as(StepVerifier::create).assertNext((actual) -> {
			assertThat(actual.getStatus()).isEqualTo(Status.UP);
			assertThat(actual.getDetails()).containsOnly(entry("database", "H2"), entry("valid", true));
		}).verifyComplete();
	}

	@Test
	void healthIndicatorWithDatabaseDown() {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.getMetadata()).willReturn(() -> "mock");
		RuntimeException exception = new RuntimeException("test");
		given(connectionFactory.create()).willReturn(Mono.error(exception));
		ConnectionFactoryHealthIndicator healthIndicator = new ConnectionFactoryHealthIndicator(connectionFactory);
		healthIndicator.health().as(StepVerifier::create).assertNext((actual) -> {
			assertThat(actual.getStatus()).isEqualTo(Status.DOWN);
			assertThat(actual.getDetails()).containsOnly(entry("database", "mock"),
					entry("error", "java.lang.RuntimeException: test"));
		}).verifyComplete();
	}

}

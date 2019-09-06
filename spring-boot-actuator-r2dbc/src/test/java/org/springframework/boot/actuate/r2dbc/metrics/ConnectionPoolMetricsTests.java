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

package org.springframework.boot.actuate.r2dbc.metrics;

import java.util.Random;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link ConnectionPoolMetrics}.
 *
 * @author Tadaya Tsuyukubo
 * @author Mark Paluch
 */
class ConnectionPoolMetricsTests {

	Tag fooTag = Tag.of("foo", "FOO");

	Tag barTag = Tag.of("bar", "BAR");

	private ConnectionFactory connectionFactory;

	@BeforeEach
	void init() {
		connectionFactory = new H2ConnectionFactory(H2ConnectionConfiguration.builder()
				.inMemory("db-" + new Random().nextInt()).option("DB_CLOSE_DELAY=-1").build());
	}

	@Test
	void metrics() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		ConnectionPool connectionPool = new ConnectionPool(
				ConnectionPoolConfiguration.builder(this.connectionFactory).initialSize(3).maxSize(7).build());

		ConnectionPoolMetrics metrics = new ConnectionPoolMetrics(connectionPool, "my-pool", Tags.of(fooTag, barTag));
		metrics.bindTo(registry);
		// acquire two connections
		connectionPool.create().as(StepVerifier::create).expectNextCount(1).verifyComplete();
		connectionPool.create().as(StepVerifier::create).expectNextCount(1).verifyComplete();
		assertGauge(registry, "r2dbc.pool.acquired", 2);
		assertGauge(registry, "r2dbc.pool.allocated", 3);
		assertGauge(registry, "r2dbc.pool.idle", 1);
		assertGauge(registry, "r2dbc.pool.pending", 0);
		assertGauge(registry, "r2dbc.pool.max.allocated", 7);
		assertGauge(registry, "r2dbc.pool.max.pending", Integer.MAX_VALUE);
	}

	private void assertGauge(SimpleMeterRegistry registry, String metric, int expectedValue) {
		Gauge gauge = registry.get(metric).gauge();
		assertThat(gauge.value()).isEqualTo(expectedValue);
		assertThat(gauge.getId().getTags()).containsExactlyInAnyOrder(Tag.of("name", "my-pool"), fooTag, barTag);
	}

}

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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for {@link ConnectionPoolMetrics}.
 *
 * @author Tadaya Tsuyukubo
 */
public class ConnectionPoolMetricsTests {

	private ConnectionFactory connectionFactory;

	@Before
	public void init() {
		connectionFactory = new H2ConnectionFactory(H2ConnectionConfiguration.builder()
				.inMemory("db-" + new Random().nextInt()).option("DB_CLOSE_DELAY=-1")
				.build());
	}

	@Test
	public void metrics() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();

		ConnectionPool connectionPool = new ConnectionPool(ConnectionPoolConfiguration.builder(this.connectionFactory)
				.initialSize(3)
				.maxSize(7)
				.build());

		Tag fooTag = Tag.of("foo", "FOO");
		Tag barTag = Tag.of("bar", "BAR");

		ConnectionPoolMetrics metrics = new ConnectionPoolMetrics(connectionPool, "my-pool", Tags.of(fooTag, barTag));
		metrics.bindTo(registry);

		// acquire two connections
		connectionPool.create().subscribe();
		connectionPool.create().subscribe();

		Gauge gauge;

		gauge = registry.get("r2dbc.pool.acquired").gauge();
		assertThat(gauge.value()).isEqualTo(2);
		assertThat(gauge.getId().getTags()).containsExactlyInAnyOrder(Tag.of("name", "my-pool"), fooTag, barTag);

		gauge = registry.get("r2dbc.pool.allocated").gauge();
		assertThat(gauge.value()).isEqualTo(3);
		assertThat(gauge.getId().getTags()).containsExactlyInAnyOrder(Tag.of("name", "my-pool"), fooTag, barTag);

		gauge = registry.get("r2dbc.pool.idle").gauge();
		assertThat(gauge.value()).isEqualTo(1);
		assertThat(gauge.getId().getTags()).containsExactlyInAnyOrder(Tag.of("name", "my-pool"), fooTag, barTag);

		gauge = registry.get("r2dbc.pool.pending").gauge();
		assertThat(gauge.value()).isEqualTo(0);
		assertThat(gauge.getId().getTags()).containsExactlyInAnyOrder(Tag.of("name", "my-pool"), fooTag, barTag);

		gauge = registry.get("r2dbc.pool.max.allocated").gauge();
		assertThat(gauge.value()).isEqualTo(7);
		assertThat(gauge.getId().getTags()).containsExactlyInAnyOrder(Tag.of("name", "my-pool"), fooTag, barTag);

		gauge = registry.get("r2dbc.pool.max.pending").gauge();
		assertThat(gauge.value()).isEqualTo(Integer.MAX_VALUE);
		assertThat(gauge.getId().getTags()).containsExactlyInAnyOrder(Tag.of("name", "my-pool"), fooTag, barTag);
	}

}

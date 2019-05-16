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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.PoolMetrics;

/**
 * A {@link MeterBinder} for a {@link ConnectionPool}.
 *
 * @author Tadaya Tsuyukubo
 */
public class ConnectionPoolMetrics implements MeterBinder {

	private final ConnectionPool pool;

	private final Iterable<Tag> tags;

	public ConnectionPoolMetrics(ConnectionPool pool, String name, Iterable<Tag> tags) {
		this.pool = pool;
		this.tags = Tags.concat(tags, "name", name);
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		this.pool.getMetrics().ifPresent(poolMetrics -> {

			Gauge.builder("r2dbc.pool.acquired", poolMetrics, PoolMetrics::acquiredSize)
					.tags(this.tags)
					.description("Size of successfully acquired connections which are in active use")
					.baseUnit("connections")
					.register(registry);

			Gauge.builder("r2dbc.pool.allocated", poolMetrics, PoolMetrics::allocatedSize)
					.tags(this.tags)
					.description("Size of allocated connections in the pool which are in active use or in idle")
					.baseUnit("connections")
					.register(registry);

			Gauge.builder("r2dbc.pool.idle", poolMetrics, PoolMetrics::idleSize)
					.tags(this.tags)
					.description("Size of idle connections in the pool")
					.baseUnit("connections")
					.register(registry);

			Gauge.builder("r2dbc.pool.pending", poolMetrics, PoolMetrics::pendingAcquireSize)
					.tags(this.tags)
					.description("Size of pending to acquire connections from underlying connection factory")
					.baseUnit("connections")
					.register(registry);

			Gauge.builder("r2dbc.pool.max.allocated", poolMetrics, PoolMetrics::getMaxAllocatedSize)
					.tags(this.tags)
					.description("Maximum size of allocated connections that this pool allows")
					.baseUnit("connections")
					.register(registry);

			Gauge.builder("r2dbc.pool.max.pending", poolMetrics, PoolMetrics::getMaxPendingAcquireSize)
					.tags(this.tags)
					.description("Maximum size of pending state to acquire connections that this pool allows")
					.baseUnit("connections")
					.register(registry);

		});
	}
}

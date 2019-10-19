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

package org.springframework.boot.actuate.autoconfigure.metrics.r2dbc;

import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.r2dbc.pool.ConnectionPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.r2dbc.ConnectionPoolMetrics;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * {@link ConnectionPool connection pools}.
 *
 * @author Tadaya Tsuyukubo
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({ MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class, ConnectionFactoryAutoConfiguration.class })
@ConditionalOnClass({ ConnectionPool.class, MeterRegistry.class })
@ConditionalOnBean({ ConnectionPool.class, MeterRegistry.class })
public class ConnectionPoolMetricsAutoConfiguration {

	@Autowired
	public void bindConnectionPoolsToRegistry(Map<String, ConnectionPool> pools, MeterRegistry registry) {
		pools.forEach((beanName, pool) -> new ConnectionPoolMetrics(pool, beanName, Tags.empty()).bindTo(registry));
	}

}

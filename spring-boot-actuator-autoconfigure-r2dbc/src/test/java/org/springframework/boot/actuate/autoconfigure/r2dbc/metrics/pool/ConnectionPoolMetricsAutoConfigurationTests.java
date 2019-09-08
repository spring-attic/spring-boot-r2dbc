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

package org.springframework.boot.actuate.autoconfigure.r2dbc.metrics.pool;

import java.util.Random;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConnectionPoolMetricsAutoConfiguration}.
 *
 * @author Tadaya Tsuyukubo
 */
class ConnectionPoolMetricsAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ConnectionPoolMetricsAutoConfiguration.class))
			.withUserConfiguration(TestConfig.class);

	@Test
	void shouldBindConnectionPoolToMeterRegistry() {
		this.contextRunner.run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			assertThat(registry.find("r2dbc.pool.acquired").gauges()).extracting(Meter::getId)
					.extracting((id) -> id.getTag("name")).containsExactlyInAnyOrder("firstPool", "secondPool");
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfig {

		@Bean
		SimpleMeterRegistry registry() {
			return new SimpleMeterRegistry();
		}

		@Bean
		ConnectionFactory connectionFactory() {
			return new H2ConnectionFactory(H2ConnectionConfiguration.builder().inMemory("db-" + new Random().nextInt())
					.option("DB_CLOSE_DELAY=-1").build());
		}

		@Bean
		ConnectionPool firstPool(ConnectionFactory connectionFactory) {
			return new ConnectionPool(ConnectionPoolConfiguration.builder(connectionFactory).build());
		}

		@Bean
		ConnectionPool secondPool(ConnectionFactory connectionFactory) {
			return new ConnectionPool(ConnectionPoolConfiguration.builder(connectionFactory).build());
		}

	}

}

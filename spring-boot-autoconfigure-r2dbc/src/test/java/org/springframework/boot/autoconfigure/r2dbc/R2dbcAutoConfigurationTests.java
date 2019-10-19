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

package org.springframework.boot.autoconfigure.r2dbc;

import io.r2dbc.client.R2dbc;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link R2dbcDataAutoConfiguration}.
 *
 * @author Mark Paluch
 */
class R2dbcAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(ConnectionFactoryAutoConfiguration.class, R2dbcAutoConfiguration.class));

	@Test
	void testDefaultConnectionFactoryExists() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(R2dbc.class));
	}

	@Test
	void shouldCreateConnectionPoolByDefault() {
		this.contextRunner
				.withPropertyValues("spring.r2dbc.url=r2dbc:simple://simpledb")
				.run((context) -> {
					R2dbcProperties properties = context.getBean(R2dbcProperties.class);
					ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);

					assertThat(properties.getPool().getEnabled()).isTrue();
					assertThat(connectionFactory).isInstanceOf(ConnectionPool.class);
				});
	}


	@Test
	void shouldCreateConnectionPoolIfPoolingIsEnabled() {
		this.contextRunner
				.withPropertyValues(
						"spring.r2dbc.pool.enabled=true",
						"spring.r2dbc.url=r2dbc:simple://simpledb")
				.run((context) -> {
					R2dbcProperties properties = context.getBean(R2dbcProperties.class);
					ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);

					assertThat(properties.getPool().getEnabled()).isTrue();
					assertThat(connectionFactory).isInstanceOf(ConnectionPool.class);
				});
	}


	@Test
	void shouldNotCreateConnectionPoolIPoolingIsfDisabled() {
		this.contextRunner
				.withPropertyValues(
						"spring.r2dbc.pool.enabled=false",
						"spring.r2dbc.url=r2dbc:simple://simpledb")
				.run((context) -> {
					R2dbcProperties properties = context.getBean(R2dbcProperties.class);
					ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);

					assertThat(properties.getPool().getEnabled()).isFalse();
					assertThat(connectionFactory).isNotInstanceOf(ConnectionPool.class);
				});
	}

}

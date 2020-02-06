/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.List;
import java.util.stream.Collectors;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

/**
 * Actual {@link ConnectionFactory} configurations imported by
 * {@link ConnectionFactoryAutoConfiguration}.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
abstract class ConnectionFactoryConfigurations {

	protected static ConnectionFactory createConnectionFactory(R2dbcProperties properties, ClassLoader classLoader,
			List<ConnectionFactoryOptionsBuilderCustomizer> optionsCustomizers) {
		return ConnectionFactoryBuilder.of(properties, () -> EmbeddedDatabaseConnection.get(classLoader))
				.configure((options) -> {
					for (ConnectionFactoryOptionsBuilderCustomizer optionsCustomizer : optionsCustomizers) {
						optionsCustomizer.customize(options);
					}
				}).build();
	}

	@Configuration(proxyBeanMethods = false)
	static class Pool {

		@Bean(destroyMethod = "dispose")
		ConnectionPool connectionFactory(R2dbcProperties properties, ResourceLoader resourceLoader,
				ObjectProvider<ConnectionFactoryOptionsBuilderCustomizer> customizers) {
			ConnectionFactory connectionFactory = createConnectionFactory(properties, resourceLoader.getClassLoader(),
					customizers.orderedStream().collect(Collectors.toList()));
			R2dbcProperties.Pool pool = properties.getPool();
			ConnectionPoolConfiguration.Builder builder = ConnectionPoolConfiguration.builder(connectionFactory)
					.maxSize(pool.getMaxSize()).initialSize(pool.getInitialSize()).maxIdleTime(pool.getMaxIdleTime());
			if (StringUtils.hasText(pool.getValidationQuery())) {
				builder.validationQuery(pool.getValidationQuery());
			}
			return new ConnectionPool(builder.build());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class Generic {

		@Bean
		ConnectionFactory connectionFactory(R2dbcProperties properties, ResourceLoader resourceLoader,
				ObjectProvider<ConnectionFactoryOptionsBuilderCustomizer> customizers) {
			return createConnectionFactory(properties, resourceLoader.getClassLoader(),
					customizers.orderedStream().collect(Collectors.toList()));
		}

	}

}

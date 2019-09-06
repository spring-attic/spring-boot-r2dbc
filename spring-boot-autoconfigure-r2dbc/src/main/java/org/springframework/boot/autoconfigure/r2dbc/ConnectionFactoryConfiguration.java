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

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Configurations for pooled and unpooled {@link ConnectionFactory} beans.
 *
 * @author Mark Paluch
 */
public class ConnectionFactoryConfiguration {

	@Configuration
	protected static class ConnectionPoolConnectionFactoryConfiguration {

		@Bean(destroyMethod = "dispose")
		ConnectionPool connectionFactory(R2dbcProperties properties) {
			ConnectionFactory connectionFactory = ConnectionFactoryBuilder.create(properties).build();
			R2dbcProperties.Pool pool = properties.getPool();
			ConnectionPoolConfiguration.Builder builder = ConnectionPoolConfiguration.builder(connectionFactory)
					.maxSize(pool.getMaxSize()).initialSize(pool.getInitialSize()).maxIdleTime(pool.getMaxIdleTime());
			if (StringUtils.hasText(pool.getValidationQuery())) {
				builder.validationQuery(pool.getValidationQuery());
			}
			return new ConnectionPool(builder.build());
		}

	}

	@Configuration
	@AutoConfigureAfter(EmbeddedDatabaseConfiguration.class)
	protected static class Generic {

		@Bean
		ConnectionFactory connectionFactory(R2dbcProperties properties) {
			return ConnectionFactoryBuilder.create(properties).build();
		}

	}

}

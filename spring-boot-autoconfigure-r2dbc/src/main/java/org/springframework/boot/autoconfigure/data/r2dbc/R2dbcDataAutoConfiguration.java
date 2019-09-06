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

package org.springframework.boot.autoconfigure.data.r2dbc;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.support.R2dbcExceptionTranslator;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link DatabaseClient}.
 *
 * @author Mark Paluch
 * @author Oliver Drotbohm
 */
@Configuration
@ConditionalOnClass(DatabaseClient.class)
@ConditionalOnMissingBean(DatabaseClient.class)
@ConditionalOnBean(ConnectionFactory.class)
@AutoConfigureAfter(ConnectionFactoryAutoConfiguration.class)
public class R2dbcDataAutoConfiguration extends AbstractR2dbcConfiguration {

	private final ConnectionFactory connectionFactory;

	public R2dbcDataAutoConfiguration(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	@Override
	public ConnectionFactory connectionFactory() {
		return this.connectionFactory;
	}

	@Bean
	@Override
	public DatabaseClient databaseClient(ReactiveDataAccessStrategy dataAccessStrategy,
			R2dbcExceptionTranslator exceptionTranslator) {
		return DatabaseClient.builder().connectionFactory(this.connectionFactory())
				.dataAccessStrategy(dataAccessStrategy).exceptionTranslator(exceptionTranslator).build();
	}

}

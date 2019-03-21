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

import javax.sql.DataSource;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link ConnectionFactory}.
 *
 * @author Mark Paluch
 */
@Configuration
@ConditionalOnClass({ConnectionFactory.class, EmbeddedDatabaseType.class})
@EnableConfigurationProperties(R2dbcProperties.class)
public class ConnectionFactoryAutoConfiguration {

	@Configuration
	@Conditional(EmbeddedDatabaseCondition.class)
	@ConditionalOnMissingBean(ConnectionFactory.class)
	@Import(EmbeddedDatabaseConfiguration.class)
	protected static class EmbeddedConfiguration {

	}

	@Configuration
	@ConditionalOnMissingBean(ConnectionFactory.class)
	@ConditionalOnProperty("spring.r2dbc.url")
	protected static class ConnectionFactoryConfiguration {

		@Bean
		ConnectionFactory connectionFactory(R2dbcProperties properties) {
			ConnectionFactoryOptions.Builder builder = ConnectionFactoryOptions
					.parse(properties.determineUrl()).mutate();
			String username = properties.determineUsername();
			if (StringUtils.hasText(username)) {
				builder.option(ConnectionFactoryOptions.USER, username);
			}
			String password = properties.determinePassword();
			if (StringUtils.hasText(password)) {
				builder.option(ConnectionFactoryOptions.PASSWORD, username);
			}
			String databaseName = properties.determineDatabaseName();
			if (StringUtils.hasText(databaseName)) {
				builder.option(ConnectionFactoryOptions.DATABASE, databaseName);
			}
			if (properties.getProperties() != null) {
				properties.getProperties()
						.forEach((key, value) -> builder
								.option(Option.valueOf(key), value));
			}
			return ConnectionFactories.get(builder.build());
		}

	}

	/**
	 * {@link Condition} to detect when an embedded {@link DataSource} type can be used.
	 */
	static class EmbeddedDatabaseCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage
					.forCondition("EmbeddedDataSource");
			EmbeddedDatabaseType type = EmbeddedDatabaseConnection
					.get(context.getClassLoader()).getType();
			if (type == null) {
				return ConditionOutcome
						.noMatch(message.didNotFind("embedded database").atAll());
			}
			return ConditionOutcome.match(message.found("embedded database").items(type));
		}

	}

}

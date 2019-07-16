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

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.spi.ConnectionFactory;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@ConditionalOnClass(ConnectionFactory.class)
@EnableConfigurationProperties(R2dbcProperties.class)
@Import(ConnectionFactoryInitializationConfiguration.class)
public class ConnectionFactoryAutoConfiguration {

	@Configuration
	@ConditionalOnClass(EmbeddedDatabaseType.class)
	@Conditional(EmbeddedDatabaseCondition.class)
	@ConditionalOnMissingBean(ConnectionFactory.class)
	@Import(EmbeddedDatabaseConfiguration.class)
	protected static class EmbeddedConfiguration {

	}

	@Configuration
	@Conditional(GenericCondition.class)
	@ConditionalOnMissingBean(ConnectionFactory.class)
	@Import(ConnectionFactoryConfiguration.Generic.class)
	protected static class GenericConnectionFactoryConfiguration {

	}

	@Configuration
	@Conditional(UnpooledConnectionUrlCondition.class)
	@ConditionalOnClass(ConnectionPool.class)
	@ConditionalOnMissingBean(ConnectionFactory.class)
	@Import(ConnectionFactoryConfiguration.ConnectionPoolConnectionFactoryConfiguration.class)
	protected static class PooledConnectionFactoryConfiguration {

	}

	/**
	 * {@link AnyNestedCondition} that checks that {@code spring.r2dbc.url}
	 * is set.
	 */
	static class EmbeddedOptOutCondition extends AnyNestedCondition {

		EmbeddedOptOutCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnProperty("spring.r2dbc.url")
		static class ExplicitUrl {
		}

	}

	/**
	 * {@link GenericCondition} that checks that {@code spring.r2dbc.url}
	 * is set and that {@link EmbeddedDatabaseCondition} does not apply.
	 */
	static class GenericCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {

			if (StringUtils
					.hasText(context.getEnvironment().getProperty("spring.r2dbc.url"))) {
				ConditionMessage.Builder message = ConditionMessage
						.forCondition("ConnectionFactory");
				return ConditionOutcome
						.match(message
								.foundExactly("a configured R2DBC Connection URL"));
			}
			return ConditionOutcome.inverse(new EmbeddedDatabaseCondition()
					.getMatchOutcome(context, metadata));
		}

	}

	/**
	 * {@link AnyNestedCondition} that checks that either {@code io.r2dbc.pool.ConnectionPool}
	 * is not on the class path or the URL defines connection pooling.
	 */
	static class GenericConnectionFactoryCondition extends AnyNestedCondition {

		GenericConnectionFactoryCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnMissingClass("io.r2dbc.pool.ConnectionPool")
		static class NoConnectionPool {
		}

		@Conditional(PooledConnectionUrlCondition.class)
		static class PooledConnectionUrl {
		}

	}

	/**
	 * {@link Condition} to detect when an embedded {@link DataSource} type can be used.
	 */
	static class EmbeddedDatabaseCondition extends SpringBootCondition {

		EmbeddedOptOutCondition pooledCondition = new EmbeddedOptOutCondition();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage
					.forCondition("EmbeddedDataSource");
			if (anyMatches(context, metadata, this.pooledCondition)) {
				return ConditionOutcome
						.noMatch(message.foundExactly("supported pooled data source"));
			}
			String type = EmbeddedDatabaseConnection
					.get(context.getClassLoader()).getType();
			if (type == null) {
				return ConditionOutcome
						.noMatch(message.didNotFind("embedded database").atAll());
			}
			if (StringUtils
					.hasText(context.getEnvironment().getProperty("spring.r2dbc.url"))) {
				return ConditionOutcome
						.noMatch(message
								.foundExactly("a configured R2DBC Connection URL"));
			}
			return ConditionOutcome.match(message.found("embedded database").items(type));
		}
	}

	/**
	 * {@link Condition} to test {@code spring.r2dbc.url} already contains pooling configuration.
	 */
	static class UnpooledConnectionUrlCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			String url = context.getEnvironment().getProperty("spring.r2dbc.url");
			if (StringUtils.isEmpty(url)) {
				return ConditionOutcome.noMatch("R2DBC Connection URL is empty");
			}
			if (url.contains(":pool:")) {
				return ConditionOutcome
						.noMatch("R2DBC Connection URL contains pooling configuration");
			}
			return ConditionOutcome
					.match("R2DBC Connection URL does not contain pooling configuration");
		}
	}

	/**
	 * {@link Condition} to test {@code spring.r2dbc.url} already contains pooling configuration.
	 */
	static class PooledConnectionUrlCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			String url = context.getEnvironment().getProperty("spring.r2dbc.url");
			if (StringUtils.isEmpty(url)) {
				return ConditionOutcome.match("R2DBC Connection URL is empty");
			}
			if (url.contains(":pool:")) {
				return ConditionOutcome
						.match("R2DBC Connection URL contains pooling configuration");
			}
			return ConditionOutcome
					.noMatch("R2DBC Connection URL does not contain pooling configuration");
		}
	}

}

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

import java.sql.Connection;
import java.util.List;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.util.StringUtils;

/**
 * Configuration for embedded data sources.
 *
 * @author Mark Paluch
 * @see ConnectionFactoryAutoConfiguration
 */
@Configuration
@ConditionalOnClass({ Connection.class, ConnectionFactory.class })
@EnableConfigurationProperties(R2dbcProperties.class)
@Import({ EmbeddedDatabaseConfiguration.WithSpringJdbcConfiguration.class,
		EmbeddedDatabaseConfiguration.NoSpringJdbcConfiguration.class })
public class EmbeddedDatabaseConfiguration implements BeanClassLoaderAware {

	private ClassLoader classLoader;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Bean
	public ConnectionFactory connectionFactory(EmbeddedDatabaseConnectionInformation embedded,
			List<ConnectionFactoryOptionsBuilderCustomizer> customizers) {
		EmbeddedDatabaseConnection connection = EmbeddedDatabaseConnection.get(this.classLoader);
		ConnectionFactoryOptions.Builder builder = ConnectionFactoryOptions.parse(connection.getUrl(embedded.getName()))
				.mutate();
		if (StringUtils.hasText(embedded.getUsername())) {
			builder.option(ConnectionFactoryOptions.USER, embedded.getUsername());
		}
		if (StringUtils.hasText(embedded.getUsername())) {
			builder.option(ConnectionFactoryOptions.PASSWORD, embedded.getPassword());
		}
		if (customizers != null) {
			for (ConnectionFactoryOptionsBuilderCustomizer customizer : customizers) {
				customizer.customize(builder);
			}
		}
		return ConnectionFactories.get(builder.build());
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(EmbeddedDatabase.class)
	@EnableConfigurationProperties(DataSourceProperties.class)
	static class WithSpringJdbcConfiguration {

		@Bean
		@ConditionalOnMissingBean
		EmbeddedDatabaseConnectionInformation embeddedDatabaseNameFromJdbc(DataSourceProperties jdbcProperties) {
			return new EmbeddedDatabaseConnectionInformation(jdbcProperties.determineDatabaseName(),
					jdbcProperties.determineUsername(), jdbcProperties.determinePassword());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("org.springframework.jdbc.datasource.embedded.EmbeddedDatabase")
	static class NoSpringJdbcConfiguration {

		@Bean
		@ConditionalOnMissingBean
		EmbeddedDatabaseConnectionInformation embeddedDatabaseNameFromR2dbc(R2dbcProperties properties) {
			return new EmbeddedDatabaseConnectionInformation(properties.determineDatabaseName(),
					properties.determineUsername(), properties.determinePassword());
		}

	}

	static class EmbeddedDatabaseConnectionInformation {

		final String name;

		final String username;

		final String password;

		EmbeddedDatabaseConnectionInformation(String name, String username, String password) {
			this.name = name;
			this.username = username;
			this.password = password;
		}

		String getName() {
			return this.name;
		}

		String getUsername() {
			return this.username;
		}

		String getPassword() {
			return this.password;
		}

	}

}

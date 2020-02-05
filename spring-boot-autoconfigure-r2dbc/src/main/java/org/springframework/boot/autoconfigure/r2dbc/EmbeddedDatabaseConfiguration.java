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

import java.sql.Connection;
import java.util.List;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Configuration for embedded data sources.
 *
 * @author Mark Paluch
 * @see ConnectionFactoryAutoConfiguration
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Connection.class, ConnectionFactory.class })
@EnableConfigurationProperties(R2dbcProperties.class)
public class EmbeddedDatabaseConfiguration implements BeanClassLoaderAware {

	private ClassLoader classLoader;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Bean
	public ConnectionFactory connectionFactory(R2dbcProperties properties,
			List<ConnectionFactoryOptionsBuilderCustomizer> customizers) {
		EmbeddedDatabaseConnection connection = EmbeddedDatabaseConnection.get(this.classLoader);
		ConnectionFactoryOptions.Builder builder = ConnectionFactoryOptions
				.parse(connection.getUrl(properties.determineDatabaseName())).mutate();
		String username = properties.determineUsername();
		if (StringUtils.hasText(username)) {
			builder.option(ConnectionFactoryOptions.USER, username);
		}
		String password = properties.determinePassword();
		if (StringUtils.hasText(password)) {
			builder.option(ConnectionFactoryOptions.PASSWORD, password);
		}
		if (customizers != null) {
			for (ConnectionFactoryOptionsBuilderCustomizer customizer : customizers) {
				customizer.customize(builder);
			}
		}
		return ConnectionFactories.get(builder.build());
	}

}

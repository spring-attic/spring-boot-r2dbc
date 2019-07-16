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

package org.springframework.boot.test.autoconfigure.r2dbc;

import java.util.UUID;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Auto-configuration for a test database.
 *
 * TODO: Reuse Embedded Database Connection created by org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration.
 *
 * @author Mark Paluch
 * @see AutoConfigureTestDatabase
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore({DataSourceAutoConfiguration.class, ConnectionFactoryAutoConfiguration.class})
public class R2dbcTestDatabaseAutoConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "spring.test.database", name = "replace", havingValue = "AUTO_CONFIGURED")
	@ConditionalOnMissingBean
	public ConnectionFactory connectionFactory(Environment environment) {
		EmbeddedDatabaseConnection connection = new EmbeddedConnectionFactoryFactory(environment)
				.getEmbeddedDatabase();
		ConnectionFactoryOptions.Builder builder = ConnectionFactoryOptions
				.parse(connection.getUrl(UUID.randomUUID().toString())).mutate();
		builder.option(ConnectionFactoryOptions.USER, "sa");
		return ConnectionFactories.get(builder.build());
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.test.database", name = "replace", havingValue = "ANY",
			matchIfMissing = true)
	public static EmbeddedConnectionFactoryBeanFactoryPostProcessor embeddedConnectionFactoryBeanFactoryPostProcessor() {
		return new EmbeddedConnectionFactoryBeanFactoryPostProcessor();
	}

	@Order(Ordered.LOWEST_PRECEDENCE)
	private static class EmbeddedConnectionFactoryBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

		private static final Log logger = LogFactory
				.getLog(EmbeddedConnectionFactoryBeanFactoryPostProcessor.class);

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			Assert.isInstanceOf(ConfigurableListableBeanFactory.class, registry,
					"Test Database Auto-configuration can only be used with a ConfigurableListableBeanFactory");
			process(registry, (ConfigurableListableBeanFactory) registry);
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		}

		private void process(BeanDefinitionRegistry registry, ConfigurableListableBeanFactory beanFactory) {
			BeanDefinitionHolder holder = getConnectionFactoryBeanDefinition(beanFactory);
			if (holder != null) {
				String beanName = holder.getBeanName();
				boolean primary = holder.getBeanDefinition().isPrimary();
				logger.info("Replacing '" + beanName + "' ConnectionFactory bean with " + (primary ? "primary " : "")
						+ "embedded version");
				registry.removeBeanDefinition(beanName);
				registry.registerBeanDefinition(beanName, createEmbeddedBeanDefinition(primary));
			}
		}

		private BeanDefinition createEmbeddedBeanDefinition(boolean primary) {
			BeanDefinition beanDefinition = new RootBeanDefinition(EmbeddedConnectionFactoryFactoryBean.class);
			beanDefinition.setPrimary(primary);
			return beanDefinition;
		}

		private BeanDefinitionHolder getConnectionFactoryBeanDefinition(ConfigurableListableBeanFactory beanFactory) {
			String[] beanNames = beanFactory.getBeanNamesForType(ConnectionFactory.class);
			if (ObjectUtils.isEmpty(beanNames)) {
				logger.warn("No ConnectionFactory beans found, embedded version will not be used");
				return null;
			}
			if (beanNames.length == 1) {
				String beanName = beanNames[0];
				BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
				return new BeanDefinitionHolder(beanDefinition, beanName);
			}
			for (String beanName : beanNames) {
				BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
				if (beanDefinition.isPrimary()) {
					return new BeanDefinitionHolder(beanDefinition, beanName);
				}
			}
			logger.warn("No primary ConnectionFactory found, embedded version will not be used");
			return null;
		}

	}

	private static class EmbeddedConnectionFactoryFactoryBean
			implements FactoryBean<ConnectionFactory>, EnvironmentAware, InitializingBean {

		private EmbeddedConnectionFactoryFactory factory;
		private ConnectionFactory connectionFactory;

		@Override
		public void setEnvironment(Environment environment) {
			this.factory = new EmbeddedConnectionFactoryFactory(environment);
			ConnectionFactoryOptions.Builder builder = ConnectionFactoryOptions
					.parse(this.factory.getEmbeddedDatabase()
							.getUrl(UUID.randomUUID().toString())).mutate();
			builder.option(ConnectionFactoryOptions.USER, "sa");
			this.connectionFactory = ConnectionFactories.get(builder.build());
		}

		@Override
		public void afterPropertiesSet() throws Exception {
		}

		@Override
		public ConnectionFactory getObject() throws Exception {
			return this.connectionFactory;
		}

		@Override
		public Class<?> getObjectType() {
			return ConnectionFactory.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

	}

	private static class EmbeddedConnectionFactoryFactory {

		private final Environment environment;

		EmbeddedConnectionFactoryFactory(Environment environment) {
			this.environment = environment;
		}

		EmbeddedDatabaseConnection getEmbeddedDatabase() {
			org.springframework.boot.jdbc.EmbeddedDatabaseConnection connection = this.environment
					.getProperty("spring.test.database.connection",
							org.springframework.boot.jdbc.EmbeddedDatabaseConnection.class, org.springframework.boot.jdbc.EmbeddedDatabaseConnection.NONE);
			EmbeddedDatabaseConnection r2dbcConnection;
			if (org.springframework.boot.jdbc.EmbeddedDatabaseConnection.NONE
					.equals(connection)) {
				r2dbcConnection = EmbeddedDatabaseConnection
						.get(getClass().getClassLoader());
			}
			else if (connection == org.springframework.boot.jdbc.EmbeddedDatabaseConnection.H2) {
				r2dbcConnection = EmbeddedDatabaseConnection.H2;
			}
			else {
				r2dbcConnection = null;
			}
			Assert.state(r2dbcConnection != EmbeddedDatabaseConnection.NONE,
					"Failed to replace ConnectionFactory with an embedded database for tests. If "
							+ "you want an embedded database please put a supported one "
							+ "on the classpath or tune the replace attribute of @AutoConfigureTestDatabase.");
			return r2dbcConnection;
		}

	}

}

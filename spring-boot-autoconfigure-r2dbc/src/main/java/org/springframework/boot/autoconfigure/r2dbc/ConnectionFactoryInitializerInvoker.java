/*
 * Copyright 2019 the original author or authors.
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

import io.r2dbc.spi.ConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceInitializationMode;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Bean to handle {@link ConnectionFactory} initialization by running {@literal schema-*.sql} on
 * {@link InitializingBean#afterPropertiesSet()} and {@literal data-*.sql} SQL scripts on
 * a {@link ConnectionFactorySchemaCreatedEvent}.
 *
 * @author Mark Paluch
 * @see ConnectionFactoryAutoConfiguration
 */
class ConnectionFactoryInitializerInvoker
		implements ApplicationListener<ConnectionFactorySchemaCreatedEvent>, InitializingBean {

	private static final Log logger = LogFactory
			.getLog(ConnectionFactoryInitializerInvoker.class);

	private final ObjectProvider<ConnectionFactory> connectionFactory;

	private final R2dbcProperties properties;

	private final ApplicationContext applicationContext;

	private ConnectionFactoryInitializer connectionFactoryInitializer;

	private boolean initialized;

	ConnectionFactoryInitializerInvoker(ObjectProvider<ConnectionFactory> connectionFactory,
			R2dbcProperties properties, ApplicationContext applicationContext) {
		this.connectionFactory = connectionFactory;
		this.properties = properties;
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {
		if (shouldSkip()) {
			logger.info("Skipping ConnectionFactory initialization because JDBC and R2DBC are configured to initialize the same embedded database");
			return;
		}
		ConnectionFactoryInitializer initializer = getConnectionFactoryInitializer();
		if (initializer != null) {
			boolean schemaCreated = this.connectionFactoryInitializer.createSchema();
			if (schemaCreated) {
				initialize(initializer);
			}
		}
	}

	private boolean shouldSkip() {
		return isR2dbcEmbedded() && isJdbcEmbedded() && wouldJdbcAndR2DbcInitializeDatabases();
	}

	private boolean wouldJdbcAndR2DbcInitializeDatabases() {
		DataSourceProperties dataSourceProperties = getDataSourceProperties();
		if (dataSourceProperties == null || dataSourceProperties
				.getInitializationMode() != DataSourceInitializationMode.EMBEDDED) {
			return false;
		}
		if (this.properties
				.getInitializationMode() != ConnectionFactoryInitializationMode.EMBEDDED) {
			return false;
		}
		return true;
	}

	@Nullable
	private DataSourceProperties getDataSourceProperties() {
		return this.applicationContext
				.getBeanProvider(DataSourceProperties.class).getIfAvailable();
	}

	private boolean isR2dbcEmbedded() {
		if (StringUtils.hasText(this.properties.getUrl())) {
			return false;
		}
		return this.applicationContext
				.getBeanNamesForType(EmbeddedDatabaseConfiguration.class).length > 0;
	}

	private boolean isJdbcEmbedded() {
		return this.applicationContext
				.getBeanNamesForType(EmbeddedDatabase.class).length > 0;
	}

	private void initialize(ConnectionFactoryInitializer initializer) {
		try {
			this.applicationContext.publishEvent(
					new ConnectionFactorySchemaCreatedEvent(initializer
							.getConnectionFactory()));
			// The listener might not be registered yet, so don't rely on it.
			if (!this.initialized && !shouldSkip()) {
				this.connectionFactoryInitializer.initSchema();
				this.initialized = true;
			}
		}
		catch (IllegalStateException ex) {
			logger.warn("Could not send event to complete ConnectionFactory initialization ("
					+ ex.getMessage() + ")");
		}
	}

	@Override
	public void onApplicationEvent(ConnectionFactorySchemaCreatedEvent event) {
		// NOTE the event can happen more than once and
		// the event datasource is not used here
		ConnectionFactoryInitializer initializer = getConnectionFactoryInitializer();
		if (!this.initialized && initializer != null && !shouldSkip()) {
			initializer.initSchema();
			this.initialized = true;
		}
	}

	private ConnectionFactoryInitializer getConnectionFactoryInitializer() {
		if (this.connectionFactoryInitializer == null) {
			ConnectionFactory connectionFactory = this.connectionFactory.getIfUnique();
			if (connectionFactory != null) {
				this.connectionFactoryInitializer = new ConnectionFactoryInitializer(connectionFactory,
						this.properties, this.applicationContext);
			}
		}
		return this.connectionFactoryInitializer;
	}

}

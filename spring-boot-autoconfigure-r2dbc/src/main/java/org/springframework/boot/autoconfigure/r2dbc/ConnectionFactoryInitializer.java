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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import io.r2dbc.spi.ConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.data.r2dbc.connectionfactory.init.DatabasePopulatorUtils;
import org.springframework.data.r2dbc.connectionfactory.init.ResourceDatabasePopulator;
import org.springframework.util.StringUtils;

/**
 * Initialize a {@link ConnectionFactory} based on a matching {@link R2dbcProperties}.
 * config.
 *
 * @author Mark Paluch
 */
class ConnectionFactoryInitializer {

	private static final Log logger = LogFactory.getLog(ConnectionFactoryInitializer.class);

	private final ConnectionFactory connectionFactory;

	private final R2dbcProperties properties;

	private final ResourceLoader resourceLoader;

	/**
	 * Create a new instance with the {@link DataSource} to initialize and its matching
	 * {@link R2dbcProperties configuration}.
	 * @param connectionFactory the connection factory to initialize
	 * @param properties the matching configuration
	 * @param resourceLoader the resource loader to use (can be null)
	 */
	ConnectionFactoryInitializer(ConnectionFactory connectionFactory, R2dbcProperties properties,
			ResourceLoader resourceLoader) {
		this.connectionFactory = connectionFactory;
		this.properties = properties;
		this.resourceLoader = (resourceLoader != null) ? resourceLoader : new DefaultResourceLoader();
	}

	/**
	 * Create a new instance with the {@link ConnectionFactory} to initialize and its
	 * matching {@link R2dbcProperties configuration}.
	 * @param connectionFactory the connectionfactory to initialize
	 * @param properties the matching configuration
	 */
	ConnectionFactoryInitializer(ConnectionFactory connectionFactory, R2dbcProperties properties) {
		this(connectionFactory, properties, null);
	}

	ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	/**
	 * Create the schema if necessary.
	 * @return {@code true} if the schema was created
	 * @see R2dbcProperties#getSchema()
	 */
	boolean createSchema() {
		List<Resource> scripts = getScripts("spring.r2dbc.schema", this.properties.getSchema(), "schema");
		if (!scripts.isEmpty()) {
			if (!isEnabled()) {
				logger.debug("Initialization disabled (not running DDL scripts)");
				return false;
			}
			String username = this.properties.getSchemaUsername();
			String password = this.properties.getSchemaPassword();
			runScripts(scripts, username, password);
		}
		return !scripts.isEmpty();
	}

	/**
	 * Initialize the schema if necessary.
	 * @see R2dbcProperties#getData()
	 */
	void initSchema() {
		List<Resource> scripts = getScripts("spring.r2dbc.data", this.properties.getData(), "data");
		if (!scripts.isEmpty()) {
			if (!isEnabled()) {
				logger.debug("Initialization disabled (not running data scripts)");
				return;
			}
			String username = this.properties.getDataUsername();
			String password = this.properties.getDataPassword();
			runScripts(scripts, username, password);
		}
	}

	private boolean isEnabled() {
		ConnectionFactoryInitializationMode mode = this.properties.getInitializationMode();
		if (mode == ConnectionFactoryInitializationMode.NEVER) {
			return false;
		}
		if (mode == ConnectionFactoryInitializationMode.EMBEDDED && !isEmbedded()) {
			return false;
		}
		return true;
	}

	private boolean isEmbedded() {
		try {
			return EmbeddedDatabaseConnection.isEmbedded(this.connectionFactory);
		}
		catch (Exception ex) {
			logger.debug("Could not determine if connection factory is embedded", ex);
			return false;
		}
	}

	private List<Resource> getScripts(String propertyName, List<String> resources, String fallback) {
		if (resources != null) {
			return getResources(propertyName, resources, true);
		}
		String platform = this.properties.getPlatform();
		List<String> fallbackResources = new ArrayList<>();
		fallbackResources.add("classpath*:" + fallback + "-" + platform + ".sql");
		fallbackResources.add("classpath*:" + fallback + ".sql");
		return getResources(propertyName, fallbackResources, false);
	}

	private List<Resource> getResources(String propertyName, List<String> locations, boolean validate) {
		List<Resource> resources = new ArrayList<>();
		for (String location : locations) {
			for (Resource resource : doGetResources(location)) {
				if (resource.exists()) {
					resources.add(resource);
				}
				else if (validate) {
					throw new InvalidConfigurationPropertyValueException(propertyName, resource,
							"The specified resource does not exist.");
				}
			}
		}
		return resources;
	}

	private Resource[] doGetResources(String location) {
		try {

			ResourcePatternResolver resourcePatternResolver = ResourcePatternUtils
					.getResourcePatternResolver(this.resourceLoader);
			List<Resource> resources = new ArrayList<>(Arrays.asList(resourcePatternResolver.getResources(location)));
			resources.sort((r1, r2) -> {
				try {
					return r1.getURL().toString().compareTo(r2.getURL().toString());
				}
				catch (IOException ex) {
					return 0;
				}
			});
			return resources.toArray(new Resource[0]);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to load resources from " + location, ex);
		}
	}

	private void runScripts(List<Resource> resources, String username, String password) {
		if (resources.isEmpty()) {
			return;
		}
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.setContinueOnError(this.properties.isContinueOnError());
		populator.setSeparator(this.properties.getSeparator());
		if (this.properties.getSqlScriptEncoding() != null) {
			populator.setSqlScriptEncoding(this.properties.getSqlScriptEncoding().name());
		}
		for (Resource resource : resources) {
			populator.addScript(resource);
		}
		ConnectionFactory connectionFactory = this.connectionFactory;
		if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
			connectionFactory = ConnectionFactoryBuilder.create(this.properties).username(username).password(password)
					.build();
		}

		// NOTE: We're blocking here
		DatabasePopulatorUtils.execute(populator, connectionFactory).block();
	}

}

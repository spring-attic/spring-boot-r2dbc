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

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.r2dbc.spi.ConnectionFactoryOptions;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for R2DBC.
 *
 * @author Mark Paluch
 * @author Andreas Killaitis
 */
@ConfigurationProperties(prefix = "spring.r2dbc")
public class R2dbcProperties implements BeanClassLoaderAware, InitializingBean {

	private ClassLoader classLoader;

	/**
	 * Name of the connectionfactory. Default to "testdb" when using an embedded database.
	 */
	private String name;

	/**
	 * Whether to generate a random datasource name.
	 */
	private boolean generateUniqueName;

	/**
	 * R2DBC URL of the database.
	 */
	private String url;

	/**
	 * Login username of the database.
	 */
	private String username;

	/**
	 * Login password of the database.
	 */
	private String password;

	/**
	 * Initialize the datasource with available DDL and DML scripts.
	 */
	private ConnectionFactoryInitializationMode initializationMode = ConnectionFactoryInitializationMode.EMBEDDED;

	/**
	 * Platform to use in the DDL or DML scripts (such as schema-${platform}.sql or
	 * data-${platform}.sql).
	 */
	private String platform = "all";

	/**
	 * Schema (DDL) script resource references.
	 */
	private List<String> schema;

	/**
	 * Username of the database to execute DDL scripts (if different).
	 */
	private String schemaUsername;

	/**
	 * Password of the database to execute DDL scripts (if different).
	 */
	private String schemaPassword;

	/**
	 * Data (DML) script resource references.
	 */
	private List<String> data;

	/**
	 * Username of the database to execute DML scripts (if different).
	 */
	private String dataUsername;

	/**
	 * Password of the database to execute DML scripts (if different).
	 */
	private String dataPassword;

	/**
	 * Whether to stop if an error occurs while initializing the database.
	 */
	private boolean continueOnError = false;

	/**
	 * Statement separator in SQL initialization scripts.
	 */
	private String separator = ";";

	/**
	 * SQL scripts encoding.
	 */
	private Charset sqlScriptEncoding;

	/**
	 * Extended R2DBC properties.
	 */
	private Map<String, String> properties;

	private Pool pool = new Pool();

	private EmbeddedDatabaseConnection embeddedDatabaseConnection = EmbeddedDatabaseConnection.NONE;

	private String uniqueName;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() {
		this.embeddedDatabaseConnection = EmbeddedDatabaseConnection.get(this.classLoader);
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isGenerateUniqueName() {
		return this.generateUniqueName;
	}

	public void setGenerateUniqueName(boolean generateUniqueName) {
		this.generateUniqueName = generateUniqueName;
	}

	/**
	 * Return the configured url or {@code null} if none was configured.
	 * @return the configured url
	 * @see #determineUrl()
	 */
	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Determine the url to use based on this configuration and the environment.
	 * @return the url to use
	 */
	public String determineUrl() {
		if (StringUtils.hasText(this.url)) {
			return this.url;
		}
		String databaseName = determineDatabaseName();
		String url = (databaseName != null) ? this.embeddedDatabaseConnection.getUrl(databaseName) : null;
		if (!StringUtils.hasText(url)) {
			throw new ConnectionFactoryBeanCreationException("Failed to determine a suitable R2DBC Connection URL",
					this, this.embeddedDatabaseConnection);
		}
		return url;
	}

	/**
	 * Determine the name to used based on this configuration.
	 * @return the database name to use or {@code null}
	 */
	public String determineDatabaseName() {
		if (this.generateUniqueName) {
			if (this.uniqueName == null) {
				this.uniqueName = UUID.randomUUID().toString();
			}
			return this.uniqueName;
		}
		if (StringUtils.hasLength(this.name)) {
			return this.name;
		}
		if (this.embeddedDatabaseConnection != EmbeddedDatabaseConnection.NONE) {
			return "testdb";
		}
		return null;
	}

	/**
	 * Return the configured username or {@code null} if none was configured.
	 * @return the configured username
	 * @see #determineUsername()
	 */
	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Determine the username to use based on this configuration and the environment.
	 * @return the username to use
	 */
	public String determineUsername() {
		if (StringUtils.hasText(this.username)) {
			return this.username;
		}
		if (EmbeddedDatabaseConnection.isEmbedded(determineDriverName())) {
			return "sa";
		}
		return this.username;
	}

	/**
	 * Return the configured password or {@code null} if none was configured.
	 * @return the configured password
	 * @see #determinePassword()
	 */
	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public ConnectionFactoryInitializationMode getInitializationMode() {
		return this.initializationMode;
	}

	public void setInitializationMode(ConnectionFactoryInitializationMode initializationMode) {
		this.initializationMode = initializationMode;
	}

	public String getPlatform() {
		return this.platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public List<String> getSchema() {
		return this.schema;
	}

	public void setSchema(List<String> schema) {
		this.schema = schema;
	}

	public String getSchemaUsername() {
		return this.schemaUsername;
	}

	public void setSchemaUsername(String schemaUsername) {
		this.schemaUsername = schemaUsername;
	}

	public String getSchemaPassword() {
		return this.schemaPassword;
	}

	public void setSchemaPassword(String schemaPassword) {
		this.schemaPassword = schemaPassword;
	}

	public List<String> getData() {
		return this.data;
	}

	public void setData(List<String> data) {
		this.data = data;
	}

	public String getDataUsername() {
		return this.dataUsername;
	}

	public void setDataUsername(String dataUsername) {
		this.dataUsername = dataUsername;
	}

	public String getDataPassword() {
		return this.dataPassword;
	}

	public void setDataPassword(String dataPassword) {
		this.dataPassword = dataPassword;
	}

	public boolean isContinueOnError() {
		return this.continueOnError;
	}

	public void setContinueOnError(boolean continueOnError) {
		this.continueOnError = continueOnError;
	}

	public String getSeparator() {
		return this.separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public Charset getSqlScriptEncoding() {
		return this.sqlScriptEncoding;
	}

	public void setSqlScriptEncoding(Charset sqlScriptEncoding) {
		this.sqlScriptEncoding = sqlScriptEncoding;
	}

	/**
	 * Determine the password to use based on this configuration and the environment.
	 * @return the password to use
	 */
	public String determinePassword() {
		if (StringUtils.hasText(this.password)) {
			return this.password;
		}
		if (EmbeddedDatabaseConnection.isEmbedded(determineDriverName())) {
			return "";
		}
		return this.password;
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public Pool getPool() {
		return this.pool;
	}

	public void setPool(Pool pool) {
		this.pool = pool;
	}

	String determineDriverName() {
		if (StringUtils.hasText(this.url)) {
			return ConnectionFactoryOptions.parse(this.url).getRequiredValue(ConnectionFactoryOptions.DRIVER);
		}
		if (this.embeddedDatabaseConnection != EmbeddedDatabaseConnection.NONE) {
			return this.embeddedDatabaseConnection.getType().toLowerCase();
		}
		throw new DataSourceBeanCreationException("Failed to determine a suitable driver", this,
				this.embeddedDatabaseConnection);
	}

	public static class DataSourceBeanCreationException extends BeanCreationException {

		private final R2dbcProperties properties;

		private final EmbeddedDatabaseConnection connection;

		DataSourceBeanCreationException(String message, R2dbcProperties properties,
				EmbeddedDatabaseConnection connection) {
			super(message);
			this.properties = properties;
			this.connection = connection;
		}

		public R2dbcProperties getProperties() {
			return this.properties;
		}

		public EmbeddedDatabaseConnection getConnection() {
			return this.connection;
		}

	}

	public static class Pool {

		/**
		 * Is pooling enabled?
		 */
		private boolean enabled = true;

		/**
		 * Idle timeout.
		 */
		private Duration maxIdleTime = Duration.ofMinutes(30);

		/**
		 * Initial connection pool size.
		 */
		private int initialSize = 10;

		/**
		 * Maximal connection pool size.
		 */
		private int maxSize = 10;

		/**
		 * Validation query.
		 */
		private String validationQuery;

		public boolean getEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public Duration getMaxIdleTime() {
			return this.maxIdleTime;
		}

		public void setMaxIdleTime(Duration maxIdleTime) {
			this.maxIdleTime = maxIdleTime;
		}

		public int getInitialSize() {
			return this.initialSize;
		}

		public void setInitialSize(int initialSize) {
			this.initialSize = initialSize;
		}

		public int getMaxSize() {
			return this.maxSize;
		}

		public void setMaxSize(int maxSize) {
			this.maxSize = maxSize;
		}

		public String getValidationQuery() {
			return this.validationQuery;
		}

		public void setValidationQuery(String validationQuery) {
			this.validationQuery = validationQuery;
		}

	}

	static class ConnectionFactoryBeanCreationException extends BeanCreationException {

		private final R2dbcProperties properties;

		private final EmbeddedDatabaseConnection embeddedDatabaseConnection;

		ConnectionFactoryBeanCreationException(String message, R2dbcProperties properties,
				EmbeddedDatabaseConnection embeddedDatabaseConnection) {
			super(message);
			this.properties = properties;
			this.embeddedDatabaseConnection = embeddedDatabaseConnection;
		}

		EmbeddedDatabaseConnection getEmbeddedDatabaseConnection() {
			return this.embeddedDatabaseConnection;
		}

		R2dbcProperties getProperties() {
			return this.properties;
		}

	}

}

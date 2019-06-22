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

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Connection details for {@link EmbeddedDatabaseType embedded databases}.
 *
 * @author Mark Paluch
 */
public enum EmbeddedDatabaseConnection {

	/**
	 * No Connection.
	 */
	NONE(null, null, null, null),

	/**
	 * H2 Database Connection.
	 */
	H2("H2", "io.r2dbc.h2.H2ConnectionFactoryProvider",
			"h2", "r2dbc:h2:mem://in-memory/%s?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");

	/**
	 * @see EmbeddedDatabaseType
	 */
	private final String type;

	private final String driverClassName;

	private final String driver;

	private final String url;

	EmbeddedDatabaseConnection(String type, String driverClassName, String driver, String url) {
		this.type = type;
		this.driverClassName = driverClassName;
		this.driver = driver;
		this.url = url;
	}

	/**
	 * Returns the driver class name.
	 *
	 * @return the driver class name
	 */
	public String getDriverClassName() {
		return this.driverClassName;
	}

	/**
	 * Returns the driver identifier.
	 *
	 * @return the driver identifier
	 */
	public String getDriver() {
		return this.driver;
	}

	/**
	 * Returns the embedded database type name for the connection.
	 *
	 * @return the database type
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * Returns the R2DBC URL for the connection using the specified {@code databaseName}.
	 *
	 * @param databaseName the name of the database
	 * @return the connection URL
	 */
	public String getUrl(String databaseName) {
		Assert.hasText(databaseName, "DatabaseName must not be empty");
		return (this.url != null) ? String.format(this.url, databaseName) : null;
	}

	/**
	 * Returns the most suitable {@link EmbeddedDatabaseConnection} for the given class
	 * loader.
	 * @param classLoader the class loader used to check for classes
	 * @return an {@link EmbeddedDatabaseConnection} or {@link #NONE}.
	 */
	public static EmbeddedDatabaseConnection get(ClassLoader classLoader) {
		for (EmbeddedDatabaseConnection candidate : EmbeddedDatabaseConnection
				.values()) {
			if (candidate != NONE && ClassUtils.isPresent(candidate.getDriverClassName(),
					classLoader)) {
				return candidate;
			}
		}
		return NONE;
	}

	/**
	 * Convenience method to determine if a given {@link ConnectionFactory} represents an embedded database type.
	 *
	 * @param connectionFactory the ConnectionFactory
	 * @return true if the connectionFactory is one of the embedded types
	 */
	public static boolean isEmbedded(ConnectionFactory connectionFactory) {
		return isEmbedded(connectionFactory.getMetadata().getName());
	}

	/**
	 * Convenience method to determine if a given driver name represents an embedded database type.
	 *
	 * @param driver the driver name
	 * @return true if the driver name is one of the embedded types
	 */
	public static boolean isEmbedded(String driver) {
		return driver != null && (driver.equalsIgnoreCase(H2.driver));
	}

}

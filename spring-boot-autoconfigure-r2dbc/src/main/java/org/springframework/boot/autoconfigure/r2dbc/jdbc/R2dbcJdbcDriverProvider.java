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

package org.springframework.boot.autoconfigure.r2dbc.jdbc;

import io.r2dbc.spi.ConnectionFactoryOptions;

/**
 * Provide JDBC-related information based on {@link ConnectionFactoryOptions}.
 *
 * @author Stephane Nicoll
 * @since 2.3.0
 */
public interface R2dbcJdbcDriverProvider {

	/**
	 * Return the fully qualified name of the driver that this instance supports.
	 * @return the driver class name
	 */
	String getDriverClassName();

	/**
	 * Attempt to infer a jdbc url based on the specified {@link ConnectionFactoryOptions
	 * options}. Return {@code null} if the options refer to a driver not supported by
	 * this instance.
	 * @param options the R2DBC connection factory options
	 * @return a JDBC url to connect to the same database or {@code null} if none could be
	 * inferred
	 */
	String inferJdbcUrl(ConnectionFactoryOptions options);

}

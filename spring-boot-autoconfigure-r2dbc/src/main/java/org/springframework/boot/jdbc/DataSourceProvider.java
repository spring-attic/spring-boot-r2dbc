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

package org.springframework.boot.jdbc;

import javax.sql.DataSource;

/**
 * Provide access to the {@link DataSource} that should be considered by
 * auto-configurations.
 *
 * @author Stephane Nicoll
 * @since 2.3.0
 */
@FunctionalInterface
public interface DataSourceProvider {

	/**
	 * Return the {@link DataSource} that should be considered by auto-configurations. If
	 * the purpose of the auto-configuration is to initialize the data source, use
	 * {@link #getInitializationDataSource()} instead.
	 * @return the datasource to use
	 */
	DataSource getDataSource();

	/**
	 * Return the {@link DataSource} to use to initialize the database.
	 * @return the datasource to use for initializing the database
	 */
	default DataSource getInitializationDataSource() {
		return getDataSource();
	}

}

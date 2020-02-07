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

import java.util.List;
import java.util.function.Supplier;

import javax.sql.DataSource;

import io.r2dbc.spi.ConnectionFactoryOptions;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.DataSourceProvider;

/**
 * A R2DBC {@link DataSourceProvider} that infers a {@link DataSource} from
 * {@linkplain ConnectionFactoryOptions connection factory options}.
 *
 * @author Stephane Nicoll
 * @since 2.3.0
 */
public class R2dbcDataSourceProvider implements DataSourceProvider, DisposableBean {

	private final ConnectionFactoryOptions options;

	private final List<R2dbcJdbcDriverProvider> r2dbcJdbcDriverProviders;

	private final Supplier<DataSourceBuilder<?>> builderFactory;

	private volatile DataSource dataSource;

	public R2dbcDataSourceProvider(ConnectionFactoryOptions options) {
		this(options, R2dbcJdbcDriverProviders.DEFAULT, DataSourceBuilder::create);
	}

	public R2dbcDataSourceProvider(ConnectionFactoryOptions options,
			List<R2dbcJdbcDriverProvider> r2dbcJdbcDriverProviders, Supplier<DataSourceBuilder<?>> builderFactory) {
		this.options = options;
		this.r2dbcJdbcDriverProviders = r2dbcJdbcDriverProviders;
		this.builderFactory = builderFactory;
	}

	@Override
	public DataSource getDataSource() {
		if (this.dataSource != null) {
			DataSourceBuilder<?> dataSourceBuilder = this.builderFactory.get();
			this.dataSource = createDataSource(dataSourceBuilder, this.options);
		}
		return this.dataSource;
	}

	@Override
	public void destroy() throws Exception {
		if (this.dataSource != null && this.dataSource instanceof AutoCloseable) {
			((AutoCloseable) this.dataSource).close();
		}
	}

	protected DataSource createDataSource(DataSourceBuilder<?> builder, ConnectionFactoryOptions options) {
		if (inferJdbcDriver(builder, options)) {
			applyAuthentication(builder, options);
			return build(builder);
		}
		throw new IllegalStateException("Failed to create DataSource, TODO");
	}

	private boolean inferJdbcDriver(DataSourceBuilder<?> builder, ConnectionFactoryOptions options) {
		for (R2dbcJdbcDriverProvider provider : this.r2dbcJdbcDriverProviders) {
			String url = provider.inferJdbcUrl(options);
			if (url != null) {
				builder.url(url);
				builder.driverClassName(provider.getDriverClassName());
				return true;
			}
		}
		return false;
	}

	/**
	 * Create a {@link DataSource} based on the specified {@linkplain DataSourceBuilder
	 * builder}.
	 * @param dataSourceBuilder the initialized builder
	 * @return a {@link DataSource}
	 */
	protected DataSource build(DataSourceBuilder<?> dataSourceBuilder) {
		return dataSourceBuilder.build();
	}

	private void applyAuthentication(DataSourceBuilder<?> builder, ConnectionFactoryOptions options) {
		if (options.hasOption(ConnectionFactoryOptions.USER)) {
			builder.username(options.getRequiredValue(ConnectionFactoryOptions.USER));
			CharSequence password = options.getValue(ConnectionFactoryOptions.PASSWORD);
			if (password != null) {
				builder.password(password.toString());
			}
		}
	}

}

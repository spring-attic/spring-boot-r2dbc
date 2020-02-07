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
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.r2dbc.EmbeddedDatabaseConnection;
import org.springframework.boot.autoconfigure.r2dbc.jdbc.R2dbcJdbcDriverProviders.H2R2dbcJdbcDriverProvider;
import org.springframework.boot.autoconfigure.r2dbc.jdbc.R2dbcJdbcDriverProviders.MySqlR2dbcJdbcDriverProvider;
import org.springframework.boot.autoconfigure.r2dbc.jdbc.R2dbcJdbcDriverProviders.PostgresqlR2dbcJdbcDriverProvider;
import org.springframework.boot.autoconfigure.r2dbc.jdbc.R2dbcJdbcDriverProviders.SqlServerR2dbcJdbcDriverProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link R2dbcJdbcDriverProviders}.
 *
 * @author Stephane Nicoll
 */
class R2dbcJdbcDriverProvidersTests {

	@Test
	void h2InMemoryJdbcUrl() {
		assertThat(new H2R2dbcJdbcDriverProvider()
				.inferJdbcUrl(ConnectionFactoryOptions.parse(EmbeddedDatabaseConnection.H2.getUrl("db"))))
						.isEqualTo("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
	}

	@Test
	void h2FileJdbcUrl() {
		assertThat(new H2R2dbcJdbcDriverProvider()
				.inferJdbcUrl(ConnectionFactoryOptions.parse(EmbeddedDatabaseConnection.H2.getUrl("db"))))
						.isEqualTo("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
	}

	@Test
	void mysqlJdbcUrlWithDefaultHost() {
		assertThat(new MySqlR2dbcJdbcDriverProvider().inferJdbcUrl(ConnectionFactoryOptions.parse("r2dbc:mysql:///db")))
				.isEqualTo("jdbc:mysql:///db");
	}

	@Test
	void mysqlJdbcUrlWithDefaultPort() {
		assertThat(new MySqlR2dbcJdbcDriverProvider()
				.inferJdbcUrl(ConnectionFactoryOptions.parse("r2dbc:mysql://host/db")))
						.isEqualTo("jdbc:mysql://host/db");
	}

	@Test
	void mysqlJdbcUrlWithDetails() {
		assertThat(new MySqlR2dbcJdbcDriverProvider()
				.inferJdbcUrl(ConnectionFactoryOptions.parse("r2dbc:mysql://host:1234/db")))
						.isEqualTo("jdbc:mysql://host:1234/db");
	}

	@Test
	void mysqlJdbcUrlWithoutDatabaseIsNotSupported() {
		assertThatIllegalStateException()
				.isThrownBy(() -> new MySqlR2dbcJdbcDriverProvider()
						.inferJdbcUrl(ConnectionFactoryOptions.parse("r2dbc:mysql:///")))
				.withMessageContaining("database");
	}

	@Test
	void mysqlJdbcUrlWithAnotherDriverDoesNotInferUrl() {
		assertThat(new MySqlR2dbcJdbcDriverProvider().inferJdbcUrl(ConnectionFactoryOptions.parse("r2dbc:test:///db")))
				.isNull();
	}

	@Test
	void postgresqlJdbcUrlWithDefaultHost() {
		assertThat(new PostgresqlR2dbcJdbcDriverProvider()
				.inferJdbcUrl(ConnectionFactoryOptions.parse("r2dbc:postgres:///db"))).isEqualTo("jdbc:postgresql:db");
	}

	@Test
	void postgresqlJdbcUrlWithDefaultPort() {
		assertThat(new PostgresqlR2dbcJdbcDriverProvider()
				.inferJdbcUrl(ConnectionFactoryOptions.parse("r2dbc:postgres://host/db")))
						.isEqualTo("jdbc:postgresql://host/db");
	}

	@Test
	void postgresqlJdbcUrlWithDetails() {
		assertThat(new PostgresqlR2dbcJdbcDriverProvider()
				.inferJdbcUrl(ConnectionFactoryOptions.parse("r2dbc:postgres://host:1234/db")))
						.isEqualTo("jdbc:postgresql://host:1234/db");
	}

	@Test
	void postgresqlJdbcUrlWithoutDatabaseIsNotSupported() {
		assertThatIllegalStateException()
				.isThrownBy(() -> new PostgresqlR2dbcJdbcDriverProvider()
						.inferJdbcUrl(ConnectionFactoryOptions.parse("r2dbc:postgres:///")))
				.withMessageContaining("database");
	}

	@Test
	void postgresqlJdbcUrlWithAnotherDriverDoesNotInferUrl() {
		assertThat(new PostgresqlR2dbcJdbcDriverProvider()
				.inferJdbcUrl(ConnectionFactoryOptions.parse("r2dbc:test:///db"))).isNull();
	}

	@Test
	void sqlServerJdbcUrlWithDefaultHost() {
		assertThat(new SqlServerR2dbcJdbcDriverProvider()
				.inferJdbcUrl(ConnectionFactoryOptions.parse("r2dbc:mssql:///db")))
						.isEqualTo("jdbc:sqlserver://;databaseName=db");
	}

	@Test
	void sqlServerJdbcUrlWithDefaultPort() {
		assertThat(new SqlServerR2dbcJdbcDriverProvider()
				.inferJdbcUrl(ConnectionFactoryOptions.parse("r2dbc:mssql://host/db")))
						.isEqualTo("jdbc:sqlserver://host;databaseName=db");
	}

	@Test
	void sqlServerJdbcUrlWithDetails() {
		assertThat(new SqlServerR2dbcJdbcDriverProvider()
				.inferJdbcUrl(ConnectionFactoryOptions.parse("r2dbc:mssql://host:1234/db")))
						.isEqualTo("jdbc:sqlserver://host:1234;databaseName=db");
	}

	@Test
	void sqlServerJdbcUrlWithoutDatabaseIsNotSupported() {
		assertThatIllegalStateException()
				.isThrownBy(() -> new SqlServerR2dbcJdbcDriverProvider()
						.inferJdbcUrl(ConnectionFactoryOptions.parse("r2dbc:mssql:///")))
				.withMessageContaining("database");
	}

	@Test
	void sqlServerJdbcUrlWithAnotherDriverDoesNotInferUrl() {
		assertThat(
				new SqlServerR2dbcJdbcDriverProvider().inferJdbcUrl(ConnectionFactoryOptions.parse("r2dbc:test:///db")))
						.isNull();
	}

}

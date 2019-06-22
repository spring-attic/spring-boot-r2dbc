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

import io.r2dbc.client.R2dbc;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import org.junit.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ConnectionFactoryInitializer}.
 *
 * @author Mark Paluch
 */
public class ConnectionFactoryInitializerTests {

	@Test
	public void initializeEmbeddedByDefault() {
		ConnectionFactory connectionFactory = createConnectionFactory();
		ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer(connectionFactory, new R2dbcProperties());
		R2dbc r2dbc = new R2dbc(connectionFactory);
		assertThat(initializer.createSchema()).isTrue();
		assertNumberOfRows(r2dbc, 0);
		initializer.initSchema();
		assertNumberOfRows(r2dbc, 1);
	}

	@Test
	public void initializeWithModeAlways() {
		ConnectionFactory connectionFactory = createConnectionFactory();
		R2dbcProperties properties = new R2dbcProperties();
		properties.setInitializationMode(ConnectionFactoryInitializationMode.ALWAYS);
		ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer(connectionFactory, properties);
		R2dbc r2dbc = new R2dbc(connectionFactory);
		assertThat(initializer.createSchema()).isTrue();
		assertNumberOfRows(r2dbc, 0);
		initializer.initSchema();
		assertNumberOfRows(r2dbc, 1);
	}

	private void assertNumberOfRows(R2dbc r2dbc, int count) {
		r2dbc.withHandle(h -> h.createQuery("SELECT COUNT(*) from BAR")
				.mapRow(row -> row.get(0)))
				.cast(Number.class)
				.map(Number::intValue)
				.as(StepVerifier::create)
				.expectNext(count)
				.verifyComplete();
	}

	@Test
	public void initializeWithModeNever() {
		ConnectionFactory connectionFactory = createConnectionFactory();
		R2dbcProperties properties = new R2dbcProperties();
		properties.setInitializationMode(ConnectionFactoryInitializationMode.NEVER);
		ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer(connectionFactory, properties);
		assertThat(initializer.createSchema()).isFalse();
	}

	@Test
	public void initializeOnlyEmbeddedByDefault() {
		ConnectionFactoryMetadata metadata = mock(ConnectionFactoryMetadata.class);
		given(metadata.getName()).willReturn("MySQL");
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.getMetadata()).willReturn(metadata);
		ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer(connectionFactory, new R2dbcProperties());
		assertThat(initializer.createSchema()).isFalse();
		verify(connectionFactory, never()).create();
	}

	private ConnectionFactory createConnectionFactory() {
		return TestConnectionFactory.get();
	}

}

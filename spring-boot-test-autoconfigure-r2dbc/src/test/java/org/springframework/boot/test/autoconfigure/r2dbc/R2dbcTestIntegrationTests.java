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

import io.r2dbc.client.R2dbc;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link R2dbcTest}.
 *
 * @author Mark Paluch
 */
@RunWith(SpringRunner.class)
@R2dbcTest
@TestPropertySource(
		properties = { "spring.r2dbc.schema=classpath:org/springframework/boot/test/autoconfigure/r2dbc/schema.sql",
				"spring.r2dbc.initialization-mode=always" })
public class R2dbcTestIntegrationTests {

	@Autowired
	private DatabaseClient databaseClient;

	@Autowired
	private R2dbc r2dbc;

	@Autowired
	private ConnectionFactory connectionFactory;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void testDatabaseClient() {
		databaseClient.execute("SELECT * FROM example").fetch().all().as(StepVerifier::create).verifyComplete();
	}

	@Test
	public void testR2dbcClient() {
		r2dbc.withHandle(h -> h.createQuery("SELECT * FROM example").mapRow(row -> row.get(0))).as(StepVerifier::create)
				.verifyComplete();
	}

	@Test
	public void replacesDefinedConnectionFactoryWithEmbeddedDefault() {
		String product = this.connectionFactory.getMetadata().getName();
		assertThat(product).isEqualTo("H2");
	}

	@Test
	public void didNotInjectExampleRepository() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.applicationContext.getBean(ExampleRepository.class));
	}

}

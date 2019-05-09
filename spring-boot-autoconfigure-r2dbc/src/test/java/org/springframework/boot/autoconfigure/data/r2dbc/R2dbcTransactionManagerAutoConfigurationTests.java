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

package org.springframework.boot.autoconfigure.data.r2dbc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.transaction.reactive.ReactiveTransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.connectionfactory.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * Tests for {@link R2dbcTransactionManager}.
 *
 * @author Mark Paluch
 * @author Oliver Drotbohm
 */
public class R2dbcTransactionManagerAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(R2dbcTransactionManagerAutoConfiguration.class,
					ReactiveTransactionAutoConfiguration.class));

	@Test
	public void noTransactionManager() {
		contextRunner.run(context -> {
			assertThat(context).doesNotHaveBean(ReactiveTransactionManager.class);
		});
	}

	@Test
	public void singleTransactionManager() {
		contextRunner.withUserConfiguration(SingleConnectionFactoryConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(TransactionalOperator.class).hasSingleBean(ReactiveTransactionManager.class);
		});
	}

	@Test
	public void transactionManagerEnabled() {
		contextRunner.withUserConfiguration(SingleConnectionFactoryConfiguration.class, BaseConfiguration.class)
				.run(context -> {
					TransactionalService bean = context.getBean(TransactionalService.class);
					bean.isTransactionActive().as(StepVerifier::create).expectNext(true).verifyComplete();
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class SingleConnectionFactoryConfiguration {

		@Bean
		ConnectionFactory connectionFactory() {
			ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
			Connection connection = mock(Connection.class);
			when(connectionFactory.create()).thenAnswer(invocation -> Mono.just(connection));
			when(connection.beginTransaction()).thenReturn(Mono.empty());
			when(connection.commitTransaction()).thenReturn(Mono.empty());
			when(connection.close()).thenReturn(Mono.empty());
			return connectionFactory;
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableTransactionManagement
	static class BaseConfiguration {

		@Bean
		public TransactionalService transactionalService() {
			return new TransactionalServiceImpl();
		}

	}

	interface TransactionalService {

		@Transactional
		Mono<Boolean> isTransactionActive();

	}

	static class TransactionalServiceImpl implements TransactionalService {

		@Override
		public Mono<Boolean> isTransactionActive() {
			return TransactionSynchronizationManager.currentTransaction()
					.map(TransactionSynchronizationManager::isActualTransactionActive);
		}

	}

}

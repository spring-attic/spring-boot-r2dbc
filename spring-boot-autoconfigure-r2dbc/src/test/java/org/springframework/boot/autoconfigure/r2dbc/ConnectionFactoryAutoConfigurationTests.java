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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Random;

import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Option;
import io.r2dbc.spi.Wrapped;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.autoconfigure.r2dbc.SimpleConnectionFactoryProvider.TestConnectionFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConnectionFactoryAutoConfiguration}.
 *
 * @author Mark Paluch
 */
class ConnectionFactoryAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ConnectionFactoryAutoConfiguration.class))
			.withInitializer((context) -> {
				ConditionEvaluationReportLoggingListener l = new ConditionEvaluationReportLoggingListener(
						LogLevel.INFO);
				l.initialize(context);
			});

	@Test
	void testDefaultPooledConnectionFactoryExists() {
		this.contextRunner.withPropertyValues("spring.r2dbc.url:r2dbc:h2:mem:///testdb-" + new Random().nextInt())
				.run((context) -> {
					assertThat(context).hasSingleBean(ConnectionFactory.class);
					ConnectionFactory bean = context.getBean(ConnectionFactory.class);
					assertThat(bean).isExactlyInstanceOf(ConnectionPool.class);
				});
	}

	@Test
	void testDefaultPooledUrlConnectionFactoryExists() {
		this.contextRunner.withPropertyValues("spring.r2dbc.url:r2dbc:pool:h2:mem:///testdb-" + new Random().nextInt())
				.run((context) -> {
					assertThat(context).hasSingleBean(ConnectionFactory.class);
					ConnectionFactory bean = context.getBean(ConnectionFactory.class);
					assertThat(bean).isExactlyInstanceOf(ConnectionPool.class);
				});
	}

	@Test
	void testDefaultGenericConnectionFactoryExists() {
		this.contextRunner
				.withPropertyValues("spring.r2dbc.url:r2dbc:h2:mem:///testdb-" + new Random().nextInt()
						+ "?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
				.withClassLoader(new FilteredClassLoader("io.r2dbc.pool")).run((context) -> {
					assertThat(context).hasSingleBean(ConnectionFactory.class);
					assertThat(context).doesNotHaveBean(EmbeddedDatabase.class);
					ConnectionFactory bean = context.getBean(ConnectionFactory.class);
					assertThat(bean).isExactlyInstanceOf(H2ConnectionFactory.class);
				});
	}

	@Test
	void testBadUrl() {
		this.contextRunner.withPropertyValues("spring.r2dbc.url:r2dbc:not-going-to-work")
				.withClassLoader(new DisableEmbeddedDatabaseClassLoader())
				.run((context) -> assertThat(context).getFailure().isInstanceOf(BeanCreationException.class));
	}

	@Test
	void testUrlConfigurationWithoutSpringJdbc() {
		this.contextRunner.withPropertyValues("spring.r2dbc.url:r2dbc:simple://foo")
				.withClassLoader(new FilteredClassLoader("org.springframework.jdbc", "io.r2dbc.pool"))
				.run((context) -> {
					assertThat(context).hasSingleBean(ConnectionFactory.class);
					ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
					assertThat(connectionFactory).isInstanceOf(TestConnectionFactory.class);
				});
	}

	@Test
	void testAdditionalOptions() {
		this.contextRunner.withPropertyValues("spring.r2dbc.url:r2dbc:simple://foo",
				"spring.r2dbc.properties.key=value", "spring.r2dbc.pool.initial-size=0").run((context) -> {
					ConnectionFactory bean = context.getBean(ConnectionFactory.class);
					TestConnectionFactory testConnectionFactory = ((Wrapped<TestConnectionFactory>) bean).unwrap();
					assertThat((Object) testConnectionFactory.options.getRequiredValue(Option.valueOf("key")))
							.isEqualTo("value");
				});
	}

	private static class DisableEmbeddedDatabaseClassLoader extends URLClassLoader {

		DisableEmbeddedDatabaseClassLoader() {
			super(new URL[0], DisableEmbeddedDatabaseClassLoader.class.getClassLoader());
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			for (EmbeddedDatabaseConnection candidate : EmbeddedDatabaseConnection.values()) {
				if (name.equals(candidate.getDriverClassName())) {
					throw new ClassNotFoundException();
				}
			}
			return super.loadClass(name, resolve);
		}

	}

}

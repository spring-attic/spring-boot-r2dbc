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

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.UUID;

import io.r2dbc.client.R2dbc;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConnectionFactoryInitializerInvoker}.
 *
 * @author Mark Paluch
 */
public class ConnectionFactoryInitializerInvokerTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations
					.of(ConnectionFactoryAutoConfiguration.class))
			.withPropertyValues("spring.r2dbc.initialization-mode=never",
					"spring.r2dbc.url:r2dbc:h2:mem:///init-" + UUID
							.randomUUID() + "?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", "spring.datasource.initialization-mode:never");

	@Before
	public void before() {
		System.setProperty("h2.delayWrongPasswordMin", "0");
		System.setProperty("h2.delayWrongPasswordMax", "1");
	}

	@After
	public void tearDown() throws Exception {
		System.clearProperty("h2.delayWrongPasswordMin");
		System.clearProperty("h2.delayWrongPasswordMax");
	}

	@Test
	public void connectionFactoryInitialized() {
		this.contextRunner
				.withPropertyValues("spring.r2dbc.initialization-mode:always")
				.run((context) -> {
					assertThat(context).hasSingleBean(ConnectionFactory.class);
					ConnectionFactory connectionFactory = context
							.getBean(ConnectionFactory.class);
					assertConnectionFactoryIsInitialized(connectionFactory, "SELECT COUNT(*) from BAR", 1);
				});
	}

	@Test
	public void initializationAppliesToCustomConnectionFactory() {
		this.contextRunner.withUserConfiguration(OneConnectionFactory.class)
				.withPropertyValues("spring.r2dbc.initialization-mode:always")
				.run((context) -> {
					assertThat(context).hasSingleBean(ConnectionFactory.class);
					assertConnectionFactoryIsInitialized(context
							.getBean(ConnectionFactory.class), "SELECT COUNT(*) from BAR", 1);
				});
	}

	private void assertConnectionFactoryIsInitialized(ConnectionFactory connectionFactory, String sql, int expectation) {
		R2dbc r2dbc = new R2dbc(connectionFactory);
		r2dbc.withHandle(h -> h.createQuery(sql)
				.mapRow(row -> row.get(0)))
				.cast(Number.class)
				.map(Number::intValue)
				.as(StepVerifier::create)
				.expectNext(expectation)
				.verifyComplete();
	}

	@Test
	public void connectionFactoryInitializedWithExplicitScript() {
		this.contextRunner
				.withPropertyValues("spring.r2dbc.initialization-mode:always",
						"spring.r2dbc.schema:" + getRelativeLocationFor("schema.sql"),
						"spring.r2dbc.data:" + getRelativeLocationFor("data.sql"))
				.run((context) -> {
					ConnectionFactory connectionFactory = context
							.getBean(ConnectionFactory.class);
					assertThat(connectionFactory).isNotNull();
					assertConnectionFactoryIsInitialized(connectionFactory, "SELECT COUNT(*) from FOO", 1);
				});
	}

	@Test
	public void connectionFactoryInitializedWithMultipleScripts() {
		this.contextRunner
				.withPropertyValues("spring.r2dbc.initialization-mode:always",
						"spring.r2dbc.schema:" + getRelativeLocationFor("schema.sql") + ","
								+ getRelativeLocationFor("another.sql"),
						"spring.r2dbc.data:" + getRelativeLocationFor("data.sql"))
				.run((context) -> {
					ConnectionFactory connectionFactory = context
							.getBean(ConnectionFactory.class);
					assertThat(connectionFactory).isNotNull();
					assertConnectionFactoryIsInitialized(connectionFactory, "SELECT COUNT(*) from FOO", 1);
					assertConnectionFactoryIsInitialized(connectionFactory, "SELECT COUNT(*) from SPAM", 0);
				});
	}

	@Test
	public void connectionFactoryInitializedWithExplicitSqlScriptEncoding() {
		this.contextRunner
				.withPropertyValues("spring.r2dbc.initialization-mode:always",
						"spring.r2dbc.sqlScriptEncoding:UTF-8",
						"spring.r2dbc.schema:" + getRelativeLocationFor("encoding-schema.sql"),
						"spring.r2dbc.data:" + getRelativeLocationFor("encoding-data.sql"))
				.run((context) -> {
					ConnectionFactory connectionFactory = context
							.getBean(ConnectionFactory.class);
					assertThat(connectionFactory).isNotNull();
					R2dbc r2dbc = new R2dbc(connectionFactory);
					r2dbc.withHandle(h -> h.createQuery("SELECT COUNT(*) from BAR")
							.mapRow(row -> row.get(0)))
							.cast(Number.class)
							.map(Number::intValue)
							.as(StepVerifier::create)
							.expectNext(2)
							.verifyComplete();
					r2dbc.withHandle(h -> h.createQuery("SELECT name from BAR WHERE id=1")
							.mapRow(row -> row.get(0)))
							.as(StepVerifier::create)
							.expectNext("bar")
							.verifyComplete();
					r2dbc.withHandle(h -> h.createQuery("SELECT name from BAR WHERE id=2")
							.mapRow(row -> row.get(0)))
							.as(StepVerifier::create)
							.expectNext("ばー")
							.verifyComplete();
				});
	}

	@Test
	public void initializationDisabled() {
		this.contextRunner.run(assertInitializationIsDisabled());
	}

	@Test
	public void initializationRunsOnceForJdbcAndR2dbcEmbedded() {
		this.contextRunner
				.withConfiguration(AutoConfigurations
						.of(EmbeddedDatabaseConfiguration.class, DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.r2dbc.url:", "spring.datasource.driver-class-name:org.h2.Driver", "spring.r2dbc.initialization-mode:embedded", "spring.datasource.initialization-mode:embedded")
				.run(context -> {
					ConnectionFactory connectionFactory = context
							.getBean(ConnectionFactory.class);
					assertThat(connectionFactory).isNotNull();
					assertConnectionFactoryIsInitialized(connectionFactory, "SELECT COUNT(*) from BAR", 1);
				});
	}

	@Test
	public void initializationDoesNotApplyWithSeveralConnectionFactorys() {
		this.contextRunner.withUserConfiguration(TwoConnectionFactories.class)
				.withPropertyValues("spring.r2dbc.initialization-mode:always")
				.run((context) -> {
					assertThat(context.getBeanNamesForType(ConnectionFactory.class))
							.hasSize(2);
					assertConnectionFactoryNotInitialized(context
							.getBean("oneConnectionFactory", ConnectionFactory.class));
					assertConnectionFactoryNotInitialized(context
							.getBean("twoConnectionFactory", ConnectionFactory.class));
				});
	}

	private ContextConsumer<AssertableApplicationContext> assertInitializationIsDisabled() {
		return (context) -> {
			assertThat(context).hasSingleBean(ConnectionFactory.class);
			ConnectionFactory connectionFactory = context
					.getBean(ConnectionFactory.class);
			context.publishEvent(new ConnectionFactorySchemaCreatedEvent(connectionFactory));
			assertConnectionFactoryNotInitialized(connectionFactory);
		};
	}

	private void assertConnectionFactoryNotInitialized(ConnectionFactory connectionFactory) {
		R2dbc r2dbc = new R2dbc(connectionFactory);
		r2dbc.withHandle(h -> h.createQuery("SELECT COUNT(*) from BAR")
				.mapRow(row -> row.get(0)))
				.as(StepVerifier::create)
				.expectErrorSatisfies(throwable -> {
					assertThat(throwable).isInstanceOf(RuntimeException.class);
					SQLException ex = (SQLException) throwable.getCause();
					int expectedCode = 42102; // object not found
					assertThat(ex.getErrorCode()).isEqualTo(expectedCode);
				})
				.verify();
	}

	@Test
	public void connectionFactoryInitializedWithSchemaCredentials() {
		this.contextRunner
				.withPropertyValues("spring.r2dbc.initialization-mode:always",
						"spring.r2dbc.sqlScriptEncoding:UTF-8",
						"spring.r2dbc.schema:" + getRelativeLocationFor("encoding-schema.sql"),
						"spring.r2dbc.data:" + getRelativeLocationFor("encoding-data.sql"),
						"spring.r2dbc.schema-username:admin", "spring.r2dbc.schema-password:admin")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.isInstanceOf(BeanCreationException.class);
				});
	}

	@Test
	public void connectionFactoryInitializedWithDataCredentials() {
		this.contextRunner
				.withPropertyValues("spring.r2dbc.initialization-mode:always",
						"spring.r2dbc.sqlScriptEncoding:UTF-8",
						"spring.r2dbc.schema:" + getRelativeLocationFor("encoding-schema.sql"),
						"spring.r2dbc.data:" + getRelativeLocationFor("encoding-data.sql"),
						"spring.r2dbc.data-username:admin", "spring.r2dbc.data-password:admin")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.isInstanceOf(BeanCreationException.class);
				});
	}

	@Test
	public void multipleScriptsAppliedInLexicalOrder() {
		new ApplicationContextRunner(() -> {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			context.setResourceLoader(new ReverseOrderResourceLoader(new DefaultResourceLoader()));
			return context;
		}).withConfiguration(AutoConfigurations
				.of(ConnectionFactoryAutoConfiguration.class))
				.withPropertyValues("spring.r2dbc.initialization-mode=always",
						"spring.r2dbc.url:r2dbc:h2:mem:///:testdb-" + new Random()
								.nextInt() + "?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
						"spring.r2dbc.schema:classpath*:" + getRelativeLocationFor("lexical-schema-*.sql"),
						"spring.r2dbc.data:classpath*:" + getRelativeLocationFor("data.sql"))
				.run((context) -> {
					ConnectionFactory connectionFactory = context
							.getBean(ConnectionFactory.class);
					assertThat(connectionFactory).isNotNull();
					assertConnectionFactoryIsInitialized(connectionFactory, "SELECT COUNT(*) from FOO", 1);
				});
	}

	@Test
	public void testConnectionFactoryInitializedWithInvalidSchemaResource() {
		this.contextRunner
				.withPropertyValues("spring.r2dbc.initialization-mode:always",
						"spring.r2dbc.schema:classpath:does/not/exist.sql")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.isInstanceOf(BeanCreationException.class);
					assertThat(context.getStartupFailure())
							.hasMessageContaining("does/not/exist.sql");
					assertThat(context.getStartupFailure())
							.hasMessageContaining("spring.r2dbc.schema");
				});
	}

	@Test
	public void connectionFactoryInitializedWithInvalidDataResource() {
		this.contextRunner
				.withPropertyValues("spring.r2dbc.initialization-mode:always",
						"spring.r2dbc.schema:" + getRelativeLocationFor("schema.sql"),
						"spring.r2dbc.data:classpath:does/not/exist.sql")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.isInstanceOf(BeanCreationException.class);
					assertThat(context.getStartupFailure())
							.hasMessageContaining("does/not/exist.sql");
					assertThat(context.getStartupFailure())
							.hasMessageContaining("spring.r2dbc.data");
				});
	}

	private String getRelativeLocationFor(String resource) {
		return ClassUtils.addResourcePathToPackagePath(getClass(), resource);
	}

	@Configuration(proxyBeanMethods = false)
	protected static class OneConnectionFactory {

		@Bean
		public ConnectionFactory oneConnectionFactory() {
			return TestConnectionFactory.get();
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class TwoConnectionFactories extends OneConnectionFactory {

		@Bean
		public ConnectionFactory twoConnectionFactory() {
			return TestConnectionFactory.get();
		}

	}

	/**
	 * {@link ResourcePatternResolver} used to ensure consistently wrong resource
	 * ordering.
	 */
	private static class ReverseOrderResourceLoader implements ResourcePatternResolver {

		private final ResourcePatternResolver resolver;

		ReverseOrderResourceLoader(ResourceLoader loader) {
			this.resolver = ResourcePatternUtils.getResourcePatternResolver(loader);
		}

		@Override
		public Resource getResource(String location) {
			return this.resolver.getResource(location);
		}

		@Override
		public ClassLoader getClassLoader() {
			return this.resolver.getClassLoader();
		}

		@Override
		public Resource[] getResources(String locationPattern) throws IOException {
			Resource[] resources = this.resolver.getResources(locationPattern);
			Arrays.sort(resources, Comparator.comparing(Resource::getFilename)
					.reversed());
			return resources;
		}

	}

}

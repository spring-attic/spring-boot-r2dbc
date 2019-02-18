/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.data.r2dbc;

import org.junit.Test;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.r2dbc.city.City;
import org.springframework.boot.autoconfigure.data.r2dbc.city.CityRepository;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.EmbeddedDatabaseConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.function.DatabaseClient;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.r2dbc.repository.config.R2dbcRepositoryConfigurationExtension;
import org.springframework.data.repository.Repository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link R2dbcRepositoriesAutoConfiguration}.
 *
 * @author Mark Paluch
 */
public class R2dbcRepositoriesAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(R2dbcRepositoriesAutoConfiguration.class))
			.withInitializer(registry -> {

				AutoConfigurationPackages
						.register((BeanDefinitionRegistry) registry.getBeanFactory(),
								R2dbcRepositoriesAutoConfigurationTests.class
										.getPackage()
										.getName());

			});

	@Test
	public void backsOffWithNoConnectionFactory() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.run((context) -> assertThat(context)
						.doesNotHaveBean(R2dbcRepositoryConfigurationExtension.class));
	}

	@Test
	public void backsOffWithNoDatabaseClientOperations() {
		this.contextRunner.withUserConfiguration(EmbeddedDatabaseConfiguration.class,
				TestConfiguration.class).run((context) -> {
			assertThat(context).doesNotHaveBean(DatabaseClient.class);
			assertThat(context)
					.doesNotHaveBean(R2dbcRepositoryConfigurationExtension.class);
		});
	}

	@Test
	public void basicAutoConfiguration() {

		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(ConnectionFactoryAutoConfiguration.class,
								DataSourceAutoConfiguration.class,
								R2dbcDataAutoConfiguration.class))
				.withUserConfiguration(TestConfiguration.class,
						EmbeddedDataSourceConfiguration.class)
				.withPropertyValues(
						"spring.datasource.schema=classpath:data-r2dbc-schema.sql",
						"spring.datasource.data=classpath:city.sql",
						"spring.datasource.generate-unique-name:true")
				.run((context) -> {
					assertThat(context).hasSingleBean(CityRepository.class);
					context.getBean(CityRepository.class).findById(2000L)
							.as(StepVerifier::create).expectNextCount(1).verifyComplete();
				});
	}

	@Test
	public void autoConfigurationWithNoRepositories() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations
								.of(ConnectionFactoryAutoConfiguration.class, EmbeddedDatabaseConfiguration.class))
				.withUserConfiguration(EmptyConfiguration.class)
				.run((context) -> {
					assertThat(context).doesNotHaveBean(Repository.class);
				});
	}

	@Test
	public void honorsUsersEnableR2dbcRepositoriesConfiguration() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(ConnectionFactoryAutoConfiguration.class,
								DataSourceAutoConfiguration.class,
								R2dbcDataAutoConfiguration.class))
				.withUserConfiguration(EnableRepositoriesConfiguration.class,
						EmbeddedDataSourceConfiguration.class)
				.withPropertyValues(
						"spring.datasource.schema=classpath:data-r2dbc-schema.sql",
						"spring.datasource.data=classpath:city.sql",
						"spring.datasource.generate-unique-name:true")
				.run((context) -> {
					assertThat(context).hasSingleBean(CityRepository.class);
					context.getBean(CityRepository.class).findById(2000L)
							.as(StepVerifier::create).expectNextCount(1).verifyComplete();
				});
	}

	private static class TestConfiguration {

	}

	@Configuration
	protected static class EmptyConfiguration {

	}

	@EnableR2dbcRepositories(basePackageClasses = City.class)
	private static class EnableRepositoriesConfiguration {

	}
}

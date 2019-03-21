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

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Option;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.r2dbc.SimpleConnectionFactoryProvider.TestConnectionFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConnectionFactoryAutoConfiguration}.
 *
 * @author Mark Paluch
 */
public class ConnectionFactoryAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations
					.of(ConnectionFactoryAutoConfiguration.class));

	@Test
	public void testDefaultConnectionFactoryExists() {
		this.contextRunner.withPropertyValues("spring.r2dbc.url:r2dbc:h2:mem:///testdb-"
				+ new Random().nextInt())
				.run((context) -> assertThat(context)
						.hasSingleBean(ConnectionFactory.class));
	}

	@Test
	public void testBadUrl() {
		this.contextRunner
				.withPropertyValues("spring.r2dbc.url:r2dbc:not-going-to-work")
				.withClassLoader(new DisableEmbeddedDatabaseClassLoader())
				.run((context) -> assertThat(context).getFailure()
						.isInstanceOf(BeanCreationException.class));
	}

	@Test
	public void testAdditionalOptions() {
		this.contextRunner
				.withPropertyValues("spring.r2dbc.url:r2dbc:simple://foo", "spring.r2dbc.properties.key=value")
				.run((context) -> {
					TestConnectionFactory bean = context
							.getBean(TestConnectionFactory.class);

					assertThat((Object) bean.options
							.getRequiredValue(Option.valueOf("key"))).isEqualTo("value");
				});
	}

	private static class DisableEmbeddedDatabaseClassLoader extends URLClassLoader {

		DisableEmbeddedDatabaseClassLoader() {
			super(new URL[0], DisableEmbeddedDatabaseClassLoader.class.getClassLoader());
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve)
				throws ClassNotFoundException {
			for (EmbeddedDatabaseConnection candidate : EmbeddedDatabaseConnection
					.values()) {
				if (name.equals(candidate.getDriverClassName())) {
					throw new ClassNotFoundException();
				}
			}
			return super.loadClass(name, resolve);
		}

	}

}

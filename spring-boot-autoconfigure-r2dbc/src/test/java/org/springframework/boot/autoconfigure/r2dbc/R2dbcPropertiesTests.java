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

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link R2dbcProperties}.
 *
 * @author Mark Paluch
 */
class R2dbcPropertiesTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(ConnectionFactoryAutoConfiguration.class, R2dbcAutoConfiguration.class));

	@Test
	void shouldReportEmbeddedDatabase() {
		this.contextRunner.run((context) -> {
			R2dbcProperties properties = context.getBean(R2dbcProperties.class);
			assertThat(properties.determineUsername()).isEqualTo("sa");
			assertThat(properties.determinePassword()).isEqualTo("");
			assertThat(properties.determineDriverName()).isEqualTo("h2");
		});
	}

	@Test
	void shouldReportCustomDriver() {
		this.contextRunner.withPropertyValues("spring.r2dbc.url=r2dbc:simple://:pool:").run((context) -> {
			R2dbcProperties properties = context.getBean(R2dbcProperties.class);
			assertThat(properties.determineUsername()).isNull();
			assertThat(properties.determinePassword()).isNull();
			assertThat(properties.determineDriverName()).isEqualTo("simple");
		});
	}

}

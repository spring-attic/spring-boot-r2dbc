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

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ConnectionFactoryBuilder}.
 *
 * @author Mark Paluch
 * @author Tadaya Tsuyukubo
 */
class ConnectionFactoryBuilderUnitTests {

	@Test
	void shouldApplyProperties() {
		R2dbcProperties properties = new R2dbcProperties();
		properties.setUrl("r2dbc:h2://foo");
		properties.setUsername("user");
		properties.setPassword("pass");
		ConnectionFactoryOptions options = ConnectionFactoryBuilder.create(properties).getOptions();
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DRIVER)).isEqualTo("h2");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.HOST)).isEqualTo("foo");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.USER)).isEqualTo("user");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PASSWORD)).isEqualTo("pass");
	}

	@Test
	void shouldApplyConfiguration() {
		R2dbcProperties properties = new R2dbcProperties();
		properties.setUrl("r2dbc:h2://foo");
		ConnectionFactoryOptions options = ConnectionFactoryBuilder.create(properties).username("user").password("pass")
				.getOptions();
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DRIVER)).isEqualTo("h2");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.HOST)).isEqualTo("foo");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.USER)).isEqualTo("user");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PASSWORD)).isEqualTo("pass");
	}

	@Test
	void shouldApplyFromUrl() {
		R2dbcProperties properties = new R2dbcProperties();
		properties.setUrl("r2dbc:h2://user:pass@local/mydb");
		properties.setUsername("someone-else");
		ConnectionFactoryOptions options = ConnectionFactoryBuilder.create(properties).getOptions();
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DRIVER)).isEqualTo("h2");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.HOST)).isEqualTo("local");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.USER)).isEqualTo("user");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.PASSWORD)).isEqualTo("pass");
		assertThat(options.getRequiredValue(ConnectionFactoryOptions.DATABASE)).isEqualTo("mydb");
	}

}

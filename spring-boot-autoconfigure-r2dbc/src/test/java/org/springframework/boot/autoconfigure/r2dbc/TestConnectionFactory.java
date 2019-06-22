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

import java.util.UUID;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;

/**
 * {@link ConnectionFactory} for testing.
 *
 * @author Mark Paluch
 */
class TestConnectionFactory {

	/**
	 * Create an in-memory database with a random name.
	 */
	public static ConnectionFactory get() {
		return get(UUID.randomUUID().toString());
	}

	/**
	 * Create an in-memory database with a given name.
	 */
	public static ConnectionFactory get(String name) {
		return ConnectionFactories.get(String
				.format("r2dbc:h2:mem:///%s?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", name));
	}

}

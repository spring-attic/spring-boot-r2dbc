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

package org.springframework.boot.test.autoconfigure.r2dbc;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Example {@link SpringBootApplication} used with {@link R2dbcTest} tests.
 *
 * @author Mark Paluch
 */
@SpringBootApplication
public class ExampleR2dbcApplication {

	@Bean
	public ConnectionFactory connectionFactory() {

		return new ConnectionFactory() {

			@Override
			public Publisher<? extends Connection> create() {
				return Mono.error(new UnsupportedOperationException());
			}

			@Override
			public ConnectionFactoryMetadata getMetadata() {
				return () -> "Custom";
			}
		};
	}
}

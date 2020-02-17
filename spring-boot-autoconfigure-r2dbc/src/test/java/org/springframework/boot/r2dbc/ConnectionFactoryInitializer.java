package org.springframework.boot.r2dbc;

import java.util.Arrays;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.r2dbc.connectionfactory.init.ResourceDatabasePopulator;

/**
 * @author Stephane Nicoll
 */
public class ConnectionFactoryInitializer implements InitializingBean {

	private final ConnectionFactory connectionFactory;

	private final ResourceDatabasePopulator databasePopulator;

	private ConnectionFactoryInitializer(ConnectionFactory connectionFactory,
			ResourceDatabasePopulator databasePopulator) {
		this.connectionFactory = connectionFactory;
		this.databasePopulator = databasePopulator;
	}

	public static Builder of(ConnectionFactory connectionFactory) {
		return new Builder(connectionFactory);
	}

	public void initialize() {
		afterPropertiesSet();
	}

	@Override
	public void afterPropertiesSet() {
		this.databasePopulator.execute(this.connectionFactory).block();
	}

	public static class Builder {

		private final ConnectionFactory connectionFactory;

		private final ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

		private ResourceLoader resourceLoader = new DefaultResourceLoader();

		private Builder(ConnectionFactory connectionFactory) {
			this.connectionFactory = connectionFactory;
		}

		public Builder withResourceLoader(ResourceLoader resourceLoader) {
			this.resourceLoader = resourceLoader;
			return this;
		}

		public Builder addScript(String script) {
			return addScript(this.resourceLoader.getResource(script));
		}

		public Builder addScript(Resource script) {
			this.databasePopulator.addScript(script);
			return this;
		}

		public Builder setScripts(String... scripts) {
			return setScripts(
					Arrays.stream(scripts).map((s) -> this.resourceLoader.getResource(s)).toArray(Resource[]::new));
		}

		public Builder setScripts(Resource... scripts) {
			this.databasePopulator.setScripts(scripts);
			return this;
		}

		public ConnectionFactoryInitializer build() {
			return new ConnectionFactoryInitializer(this.connectionFactory, this.databasePopulator);
		}

	}

}

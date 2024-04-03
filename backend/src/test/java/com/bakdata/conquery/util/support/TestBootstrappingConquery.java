package com.bakdata.conquery.util.support;

import java.io.File;
import java.io.IOException;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.logging.common.BootstrapLogging;
import io.dropwizard.logging.common.DropwizardLayout;
import jakarta.validation.Validator;

public class TestBootstrappingConquery extends Conquery {

	public static Bootstrap<ConqueryConfig> createTestBootstrapConquery(File tmpDir) {
		final Bootstrap<ConqueryConfig> bootstrap = new Bootstrap<>(new TestBootstrappingConquery());

		bootstrap.setConfigurationFactoryFactory(new DefaultConfigurationFactoryFactory<>() {
			@Override
			public ConfigurationFactory<ConqueryConfig> create(Class<ConqueryConfig> klass, Validator validator, ObjectMapper objectMapper, String propertyPrefix) {
				return new YamlConfigurationFactory<ConqueryConfig>(
						klass,
						validator,
						configureObjectMapper(objectMapper.copy()),
						propertyPrefix
				) {
					@Override
					protected ConqueryConfig build(JsonNode node, String path) throws IOException, ConfigurationException {
						final ConqueryConfig config = super.build(node, path);
						ConfigOverride.configurePathsAndLogging(config, tmpDir);
						return config;
					}
				};
			}
		});
		return bootstrap;
	}

	@Override
	protected void bootstrapLogging() {
		BootstrapLogging.bootstrap(bootstrapLogLevel(), (ctx, timeZone) -> {
			DropwizardLayout layout = new DropwizardLayout(ctx, timeZone);
			layout.setPattern(TestLoggingFactory.LOG_PATTERN);
			return layout;
		});
	}
}

package com.bakdata.conquery;

import jakarta.validation.Validator;

import ch.qos.logback.classic.Level;
import com.bakdata.conquery.commands.DistributedStandaloneCommand;
import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.MigrateCommand;
import com.bakdata.conquery.commands.PreprocessorCommand;
import com.bakdata.conquery.commands.RecodeStoreCommand;
import com.bakdata.conquery.commands.ShardCommand;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.metrics.prometheus.PrometheusBundle;
import com.bakdata.conquery.mode.Manager;
import com.bakdata.conquery.mode.ManagerProvider;
import com.bakdata.conquery.mode.cluster.ClusterManagerProvider;
import com.bakdata.conquery.mode.local.LocalManagerProvider;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.JsonConfigurationFactory;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@Slf4j
@RequiredArgsConstructor
@Getter
public class Conquery extends Application<ConqueryConfig> {

	private final String name;

	public Conquery() {
		this("Conquery");
	}

	public static void main(String... args) throws Exception {
		new Conquery().run(args);
	}

	@Override
	public void initialize(Bootstrap<ConqueryConfig> bootstrap) {
		final ObjectMapper confMapper = bootstrap.getObjectMapper();
		Jackson.configure(confMapper);

		// main config file is json
		bootstrap.setConfigurationFactoryFactory(JsonConfigurationFactory::new);

		bootstrap.addCommand(new ShardCommand());
		bootstrap.addCommand(new PreprocessorCommand());
		bootstrap.addCommand(new DistributedStandaloneCommand());
		bootstrap.addCommand(new RecodeStoreCommand());
		bootstrap.addCommand(new MigrateCommand());

		MutableInjectableValues injectableValues = (MutableInjectableValues) confMapper.getInjectableValues();
		injectableValues.add(Validator.class, bootstrap.getValidatorFactory().getValidator());
		injectableValues.add(MetricRegistry.class, bootstrap.getMetricRegistry());

		// do some setup in other classes after initialization but before running a
		// command
		bootstrap.addBundle(new ConfiguredBundle<>() {

			@Override
			public void initialize(Bootstrap<?> bootstrap) {
				// Allow overriding of config from environment variables.
				bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
						bootstrap.getConfigurationSourceProvider(), StringSubstitutor.createInterpolator()));
			}

			@Override
			public void run(ConqueryConfig configuration, Environment environment) {
				configuration.configureObjectMapper(environment.getObjectMapper());

				environment.jersey().register(new AbstractBinder() {
					@Override
					protected void configure() {
						bind(environment.getValidator()).to(Validator.class);
						bind(configuration).to(ConqueryConfig.class);
					}
				});
			}
		});

		bootstrap.addBundle(new PrometheusBundle());
	}

	@Override
	protected Level bootstrapLogLevel() {
		return Level.INFO;
	}

	@Override
	public void run(ConqueryConfig configuration, Environment environment) throws Exception {
		ManagerProvider provider = configuration.getSqlConnectorConfig().isEnabled() ?
								   new LocalManagerProvider() : new ClusterManagerProvider();
		Manager manager = provider.provideManager(configuration, environment);

		ManagerNode managerNode = new ManagerNode();

		managerNode.run(manager);
	}
}

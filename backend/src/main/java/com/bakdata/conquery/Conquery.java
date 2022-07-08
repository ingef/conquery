package com.bakdata.conquery;

import javax.validation.Validator;

import ch.qos.logback.classic.Level;
import com.bakdata.conquery.commands.CollectEntitiesCommand;
import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.MigrateCommand;
import com.bakdata.conquery.commands.PreprocessorCommand;
import com.bakdata.conquery.commands.RecodeStoreCommand;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.commands.StandaloneCommand;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Application;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.configuration.JsonConfigurationFactory;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@Slf4j
@RequiredArgsConstructor
@Getter
public class Conquery extends Application<ConqueryConfig> {

	private final String name;
	@Setter
	private ManagerNode manager;

	public Conquery() {
		this("Conquery");
	}

	@Override
	public void initialize(Bootstrap<ConqueryConfig> bootstrap) {
		final ObjectMapper confMapper = bootstrap.getObjectMapper();
		Jackson.configure(confMapper);

		// main config file is json
		bootstrap.setConfigurationFactoryFactory(JsonConfigurationFactory::new);

		bootstrap.addCommand(new ShardNode());
		bootstrap.addCommand(new PreprocessorCommand());
		bootstrap.addCommand(new CollectEntitiesCommand());
		bootstrap.addCommand(new StandaloneCommand(this));
		bootstrap.addCommand(new RecodeStoreCommand());
		bootstrap.addCommand(new MigrateCommand());

		((MutableInjectableValues) confMapper.getInjectableValues()).add(Validator.class, bootstrap.getValidatorFactory().getValidator());

		// do some setup in other classes after initialization but before running a
		// command
		bootstrap.addBundle(new ConfiguredBundle<>() {

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

			@Override
			public void initialize(Bootstrap<?> bootstrap) {
				// Allow overriding of config from environment variables.
				bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
						bootstrap.getConfigurationSourceProvider(), StringSubstitutor.createInterpolator()));
			}
		});
	}

	@Override
	protected Level bootstrapLogLevel() {
		return Level.INFO;
	}

	@Override
	public void run(ConqueryConfig configuration, Environment environment) throws Exception {
		if (manager == null) {
			manager = new ManagerNode();
		}
		manager.run(configuration, environment);
	}

	public static void main(String... args) throws Exception {
		new Conquery().run(args);
	}
}

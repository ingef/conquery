package com.bakdata.conquery;

import java.nio.charset.StandardCharsets;

import javax.tools.ToolProvider;
import javax.validation.Validator;

import ch.qos.logback.classic.Level;
import com.bakdata.conquery.commands.CollectEntitiesCommand;
import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.PreprocessorCommand;
import com.bakdata.conquery.commands.RecodeStoreCommand;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.commands.StandaloneCommand;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.util.DateFormats;
import com.bakdata.conquery.util.UrlRewriteBundle;
import io.dropwizard.Application;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.JsonConfigurationFactory;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.servlets.assets.AssetServlet;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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
		Jackson.configure(bootstrap.getObjectMapper());
		// check for java compiler, needed for the class generation
		if (ToolProvider.getSystemJavaCompiler() == null) {
			throw new IllegalStateException("Conquery requires to be run on either a JDK or a ServerJRE");
		}

		// main config file is json
		bootstrap.setConfigurationFactoryFactory(JsonConfigurationFactory::new);

		bootstrap.addCommand(new ShardNode());
		bootstrap.addCommand(new PreprocessorCommand());
		bootstrap.addCommand(new CollectEntitiesCommand());
		bootstrap.addCommand(new StandaloneCommand(this));
		bootstrap.addCommand(new RecodeStoreCommand());

		((MutableInjectableValues)bootstrap.getObjectMapper().getInjectableValues()).add(Validator.class, bootstrap.getValidatorFactory().getValidator());

		// do some setup in other classes after initialization but before running a
		// command
		bootstrap.addBundle(new ConfiguredBundle<ConqueryConfig>() {

			@Override
			public void run(ConqueryConfig configuration, Environment environment) throws Exception {
				Jackson.configure(environment.getObjectMapper(),configuration);
			}

			@Override
			public void initialize(Bootstrap<?> bootstrap) {
				// Allow overriding of config from environment variables.
				bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
						bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
			}
		});
		// register frontend
		registerFrontend(bootstrap);
	}

	protected void registerFrontend(Bootstrap<ConqueryConfig> bootstrap) {
		bootstrap.addBundle(new UrlRewriteBundle());
		bootstrap.addBundle(new ConfiguredBundle<ConqueryConfig>() {
			@Override
			public void run(ConqueryConfig configuration, Environment environment) throws Exception {
				String uriPath = "/app/";
				environment.servlets()
						.addServlet("app", new AssetServlet("/frontend/app/", uriPath, "static/index.html", StandardCharsets.UTF_8))
						.addMapping(uriPath + '*');
			}

			@Override
			public void initialize(Bootstrap<?> bootstrap) {
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

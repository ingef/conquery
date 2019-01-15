package com.bakdata.conquery;

import javax.tools.ToolProvider;

import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.commands.PreprocessorCommand;
import com.bakdata.conquery.commands.SlaveCommand;
import com.bakdata.conquery.commands.StandaloneCommand;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.preproc.DateFormats;

import ch.qos.logback.classic.Level;
import io.dropwizard.Application;
import io.dropwizard.Bundle;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.JsonConfigurationFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Getter
public class Conquery extends Application<ConqueryConfig> {

	private final String name;
	private MasterCommand master;

	public Conquery() {
		this("Conquery");
	}

	@Override
	public void initialize(Bootstrap<ConqueryConfig> bootstrap) {
		Jackson.configure(bootstrap.getObjectMapper());
		//check for java compiler, needed for the class generation
		if (ToolProvider.getSystemJavaCompiler() == null) {
			throw new IllegalStateException("Conquery requires to be run on either a JDK or a ServerJRE");
		}

		//main config file is json
		bootstrap.setConfigurationFactoryFactory(JsonConfigurationFactory::new);

		bootstrap.addCommand(new SlaveCommand());
		bootstrap.addCommand(new PreprocessorCommand());
		bootstrap.addCommand(new StandaloneCommand(this));

		//do some setup in other classes after initialization but before running a command
		bootstrap.addBundle(new ConfiguredBundle<ConqueryConfig>() {
			@Override
			public void run(ConqueryConfig configuration, Environment environment) throws Exception {
				//see #142  configuration.initializeDatePatterns();
			}

			@Override
			public void initialize(Bootstrap<?> bootstrap) {
			}
		});
		//register frontend
		bootstrap.addBundle(new AssetsBundle("/frontend/app", "/", "index.de.html"));

		//freemarker support
		bootstrap.addBundle(new ViewBundle<>());
		bootstrap.addBundle(new Bundle() {
			@Override
			public void run(Environment environment) {
				DateFormats.initialize(new String[0]);
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
		master = new MasterCommand();
		master.run(configuration, environment);
	}

	public static void main(String... args) throws Exception {
		new Conquery().run(args);
	}
}

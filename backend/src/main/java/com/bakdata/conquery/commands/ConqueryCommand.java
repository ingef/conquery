package com.bakdata.conquery.commands;

import com.bakdata.conquery.models.config.ConqueryConfig;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.inf.Namespace;
import org.eclipse.jetty.util.component.ContainerLifeCycle;

@Slf4j
public abstract class ConqueryCommand extends ConfiguredCommand<ConqueryConfig> {
	
	/**
	 * Creates a new environment command.
	 *
	 * @param name		the name of the command, used for command line invocation
	 */
	protected ConqueryCommand(String name, String description) {
		super(name, description);
	}

	@Override
	protected void run(Bootstrap<ConqueryConfig> bootstrap, Namespace namespace, ConqueryConfig configuration) throws Exception {
		final Environment environment = new Environment(bootstrap.getApplication().getName(),
														bootstrap.getObjectMapper(),
														bootstrap.getValidatorFactory(),
														bootstrap.getMetricRegistry(),
														bootstrap.getClassLoader(),
														bootstrap.getHealthCheckRegistry(),
														configuration);
		configuration.getMetricsFactory().configure(environment.lifecycle(),
													bootstrap.getMetricRegistry());
		configuration.getServerFactory().configure(environment);

		bootstrap.run(configuration, environment);
		
		ContainerLifeCycle lifeCycle = new ContainerLifeCycle();
		try {
			run(environment, namespace, configuration);
			environment.lifecycle().attach(lifeCycle);
			lifeCycle.start();
			
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						lifeCycle.stop();
					}
					catch (Exception e) {
						log.error("Interrupted during shutdown", e);
					}
				}
			});
		}
		catch(Throwable t) {
			log.error("Uncaught Exception in "+getName(), t);
			lifeCycle.stop();
			throw t;
		}
	}

	/**
	 * Runs the command with the given {@link Environment} and {@link ConqueryConfig}.
	 *
	 * @param environment   the configured environment
	 * @param namespace	 the parsed command line namespace
	 * @param configuration the configuration object
	 * @throws Exception if something goes wrong
	 */
	protected abstract void run(Environment environment, Namespace namespace, ConqueryConfig configuration) throws Exception;

}

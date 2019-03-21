package com.bakdata.conquery.commands;

import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.component.ContainerLifeCycle;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.util.DebugMode;
import com.google.common.util.concurrent.Uninterruptibles;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.inf.Namespace;

@Slf4j
public abstract class ConqueryCommand extends ConfiguredCommand<ConqueryConfig> {
	
	/**
	 * Creates a new environment command.
	 *
	 * @param application	 the application providing this command
	 * @param name		the name of the command, used for command line invocation
	 */
	protected ConqueryCommand(String name, String description) {
		super(name, description);
	}

	@Override
	protected void run(Bootstrap<ConqueryConfig> bootstrap, Namespace namespace, ConqueryConfig configuration) throws Exception {
		final Environment environment = new Environment(bootstrap.getApplication().getName(),
														bootstrap.getObjectMapper(),
														bootstrap.getValidatorFactory().getValidator(),
														bootstrap.getMetricRegistry(),
														bootstrap.getClassLoader(),
														bootstrap.getHealthCheckRegistry());
		configuration.getMetricsFactory().configure(environment.lifecycle(),
													bootstrap.getMetricRegistry());
		configuration.getServerFactory().configure(environment);

		bootstrap.run(configuration, environment);
		
		ContainerLifeCycle lifeCycle = new ContainerLifeCycle();
		try {
			if(configuration.getDebugMode() != null) {
				DebugMode.setActive(configuration.getDebugMode());
			}
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
			Uninterruptibles.sleepUninterruptibly(Long.MAX_VALUE, TimeUnit.DAYS);
		}
		catch(Throwable t) {
			log.error("Uncaught Exception in "+getName(), t);
			lifeCycle.stop();
			throw t;
		}
	}

	/**
	 * Runs the command with the given {@link Environment} and {@link Configuration}.
	 *
	 * @param environment   the configured environment
	 * @param namespace	 the parsed command line namespace
	 * @param configuration the configuration object
	 * @throws Exception if something goes wrong
	 */
	protected abstract void run(Environment environment, Namespace namespace, ConqueryConfig configuration) throws Exception;
}

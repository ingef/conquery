package com.bakdata.conquery.util.support;

import com.bakdata.conquery.models.config.ConqueryConfig;

import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.testing.POJOConfigurationFactory;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

//see #169  this is a workaround for https://github.com/dropwizard/dropwizard/issues/2496
public class TestCommandWrapper extends Command {

	private Command parent;
	private ConqueryConfig config;

	protected TestCommandWrapper(ConqueryConfig config, Command parent) {
		super(parent.getName(), parent.getDescription());
		this.parent = parent;
		this.config = config;
	}

	@Override
	public void configure(Subparser subparser) {
		parent.configure(subparser);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public void run(Bootstrap bootstrap, Namespace namespace) throws Exception {
		bootstrap.setConfigurationFactoryFactory(
			(klass, validator, objectMapper, propertyPrefix) ->
				new POJOConfigurationFactory<ConqueryConfig>(config)
		);
		parent.run(bootstrap, namespace);
	}
}
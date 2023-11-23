package com.bakdata.conquery.commands;

import java.util.List;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.mode.Manager;
import com.bakdata.conquery.models.config.ConqueryConfig;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;

public interface StandaloneCommand {

	void startStandalone(Environment environment, Namespace namespace, ConqueryConfig config) throws Exception;

	Manager getManager();

	List<ShardNode> getShardNodes();

	void run(Bootstrap<ConqueryConfig> bootstrap, Namespace namespace, ConqueryConfig configuration) throws Exception;

	Conquery getConquery();

	ManagerNode getManagerNode();

	Environment getEnvironment();

}

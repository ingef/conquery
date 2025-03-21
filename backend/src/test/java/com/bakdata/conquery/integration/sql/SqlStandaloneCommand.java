package com.bakdata.conquery.integration.sql;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.commands.StandaloneCommand;
import com.bakdata.conquery.integration.sql.dialect.TestSqlDialectFactory;
import com.bakdata.conquery.mode.DelegateManager;
import com.bakdata.conquery.mode.local.LocalManagerProvider;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.worker.LocalNamespace;
import com.bakdata.conquery.util.commands.NoOpConquery;
import com.bakdata.conquery.util.io.ConqueryMDC;
import io.dropwizard.core.cli.ServerCommand;
import io.dropwizard.core.setup.Environment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.inf.Namespace;

@Slf4j
@Getter
public class SqlStandaloneCommand extends ServerCommand<ConqueryConfig> implements StandaloneCommand {

	private final ManagerNode managerNode = new ManagerNode();
	private DelegateManager<LocalNamespace> manager;

	public SqlStandaloneCommand() {
		super(new NoOpConquery(), "standalone", "starts a sql server and a client at the same time.");
	}

	@Override
	public List<ShardNode> getShardNodes() {
		return Collections.emptyList();
	}

	@Override
	protected void run(Environment environment, Namespace namespace, ConqueryConfig configuration) throws Exception {
		ConqueryMDC.setLocation("ManagerNode");
		log.debug("Starting ManagerNode");
		this.manager = new LocalManagerProvider(new TestSqlDialectFactory()).provideManager(configuration, environment);
		managerNode.run(manager);
		// starts the Jersey Server
		log.debug("Starting REST Server");
		ConqueryMDC.setLocation(null);
		super.run(environment, namespace, configuration);
	}
}

package com.bakdata.conquery.integration.sql;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.commands.StandaloneCommand;
import com.bakdata.conquery.integration.sql.dialect.HanaSqlIntegrationTests;
import com.bakdata.conquery.integration.sql.dialect.PostgreSqlIntegrationTests;
import com.bakdata.conquery.mode.DelegateManager;
import com.bakdata.conquery.mode.local.LocalManagerProvider;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.worker.LocalNamespace;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.util.io.ConqueryMDC;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.inf.Namespace;
import org.jooq.DSLContext;

@Slf4j
@Getter
public class SqlStandaloneCommand extends io.dropwizard.cli.ServerCommand<ConqueryConfig> implements StandaloneCommand {

	private final Conquery conquery;
	private ManagerNode managerNode = new ManagerNode();
	private DelegateManager<LocalNamespace> manager;
	private Environment environment;

	public SqlStandaloneCommand(Conquery conquery) {
		super(conquery, "standalone", "starts a sql server and a client at the same time.");
		this.conquery = conquery;
	}

	@Override
	public void startStandalone(Environment environment, Namespace namespace, ConqueryConfig config) throws Exception {
		ConqueryMDC.setLocation("ManagerNode");
		log.debug("Starting ManagerNode");
		this.manager = new TestLocalManagerProvider().provideManager(config, environment);
		this.conquery.setManagerNode(managerNode);
		this.conquery.run(manager);
		// starts the Jersey Server
		log.debug("Starting REST Server");
		ConqueryMDC.setLocation(null);
		super.run(environment, namespace, config);
	}

	@Override
	public List<ShardNode> getShardNodes() {
		return Collections.emptyList();
	}

	@Override
	public void run(Bootstrap<ConqueryConfig> bootstrap, Namespace namespace, ConqueryConfig configuration) throws Exception {
		environment = new Environment(
				bootstrap.getApplication().getName(),
				bootstrap.getObjectMapper(),
				bootstrap.getValidatorFactory(),
				bootstrap.getMetricRegistry(),
				bootstrap.getClassLoader(),
				bootstrap.getHealthCheckRegistry(),
				configuration
		);
		configuration.getMetricsFactory().configure(environment.lifecycle(), bootstrap.getMetricRegistry());
		configuration.getServerFactory().configure(environment);

		bootstrap.run(configuration, environment);
		startStandalone(environment, namespace, configuration);
	}

	private static class TestLocalManagerProvider extends LocalManagerProvider {
		@Override
		protected SqlDialect createSqlDialect(SqlConnectorConfig sqlConnectorConfig, DSLContext dslContext) {
			return switch (sqlConnectorConfig.getDialect()) {
				case POSTGRESQL -> new PostgreSqlIntegrationTests.TestPostgreSqlDialect(dslContext);
				case HANA -> new HanaSqlIntegrationTests.TestHanaDialect(dslContext);
			};
		}
	}

}

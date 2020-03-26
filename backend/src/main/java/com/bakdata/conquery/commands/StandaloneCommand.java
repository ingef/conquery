package com.bakdata.conquery.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.util.Wait;
import com.bakdata.conquery.util.io.Cloner;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.cli.ServerCommand;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.inf.Namespace;

@Slf4j
@Getter
public class StandaloneCommand extends ServerCommand<ConqueryConfig> {

	private final Conquery conquery;
	private MasterCommand master;

	private final List<SlaveCommand> slaves = new Vector<>();

	public StandaloneCommand(Conquery conquery) {
		super(conquery, "standalone", "starts a server and a client at the same time.");
		this.conquery = conquery;
	}

	// this must be overridden so that
//	@Override
//	protected void run(Bootstrap<ConqueryConfig> bootstrap, Namespace namespace, ConqueryConfig configuration) throws Exception {
//
//
//		bootstrap.run(configuration, environment);
//		startStandalone(environment, namespace, configuration);
//	}

	@Override
	protected void run(Environment environment, Namespace namespace, ConqueryConfig config) throws Exception {
		// start master
		ConqueryMDC.setLocation("Master");
		log.debug("Starting Master");
		ConqueryConfig masterConfig = Cloner.clone(config);
		masterConfig.getStorage().setDirectory(new File(masterConfig.getStorage().getDirectory(), "master"));
		masterConfig.getStorage().getDirectory().mkdir();
		conquery.run(masterConfig, environment);


		//create thread pool to start multiple slaves at the same time
		ListeningExecutorService starterPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(
			config.getStandalone().getNumberOfSlaves(),
			new ThreadFactoryBuilder()
				.setNameFormat("Slave Storage Loader %d")
				.setUncaughtExceptionHandler((t, e) -> {
					ConqueryMDC.setLocation(t.getName());
					log.error(t.getName()+" failed to init storage of slave", e);
				})
				.build()
		));


		// Start master command
		master = new MasterCommand(conquery);

		List<ListenableFuture<SlaveCommand >> tasks = new ArrayList<>();

		for (int i = 0; i < config.getStandalone().getNumberOfSlaves(); i++) {

			final int id = i;

			tasks.add(starterPool.submit(() -> {

				ConqueryMDC.setLocation("Slave " + id);
				ConqueryConfig clonedEnv = Cloner.clone(config);

				clonedEnv.getStorage().setDirectory(new File(clonedEnv.getStorage().getDirectory(), "slave_" + id));
				clonedEnv.getStorage().getDirectory().mkdir();

				// TODO: 05.03.2020 Check if this works in standalone and master/slave
//				final DefaultServerFactory slaveServlet = new DefaultServerFactory();
//				final ConnectorFactory connectorFactory = HttpConnectorFactory.admin();
//				((HttpConnectorFactory) connectorFactory).setPort(0); // Force random allocation to avoid collision.
//				slaveServlet.setAdminConnectors(Collections.singletonList(connectorFactory));
//				clonedEnv.setSlaveServlet(slaveServlet);

				SlaveCommand sc = new SlaveCommand(conquery);
				sc.setLabel("slave_" + id);
				this.slaves.add(sc);
				sc.run(environment, namespace, clonedEnv);

//				Wait.builder().attempts(20).stepTime(1000).build().until(sc::getContext);

				return sc;
			}));
		}

		ConqueryMDC.setLocation("Master");


		log.debug("Waiting for slaves to start");
		starterPool.shutdown();
		starterPool.awaitTermination(1, TimeUnit.HOURS);
		//catch exceptions on tasks


		boolean failed = false;
		for(Future<SlaveCommand> f : tasks) {
			try {
				var value = f.get();
				log.info("{}", value);
			}
			catch(ExecutionException e) {
				log.error("Failed during slave creation", e);
				failed = true;
			}
		}


		if(failed) {
			System.exit(-1);
		}

		master.run(environment, namespace, config);
	}
}

package com.bakdata.conquery.commands;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.mode.cluster.ClusterConnectionShard;
import com.bakdata.conquery.mode.cluster.InternalMapperFactory;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.worker.ShardWorkers;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.tasks.LoadStorageTask;
import com.bakdata.conquery.util.io.ConqueryMDC;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This node holds a shard of data (in so called {@link Worker}s for the different datasets in conquery.
 * It delegates incomming queries to the corresponding worker and is responsible for the network communication
 * to the {@link ManagerNode}.
 */
@Slf4j
@Getter
public class ShardNode implements ConfiguredBundle<ConqueryConfig> {

	public static final String DEFAULT_NAME = "shard-node";

	private final String name;
	@Setter
	private ShardWorkers workers;
	private ClusterConnectionShard clusterConnection;

	public ShardNode() {
		this(DEFAULT_NAME);
	}

	public ShardNode(String name) {
		this.name = name;
	}


	@Override
	public void run(ConqueryConfig config, Environment environment) throws Exception {
		LifecycleEnvironment lifecycle = environment.lifecycle();


		InternalMapperFactory internalMapperFactory = new InternalMapperFactory(config, environment.getValidator());
		workers = new ShardWorkers(
				config.getQueries().getExecutionPool(),
				internalMapperFactory,
				config.getCluster().getEntityBucketSize(),
				config.getQueries().getSecondaryIdSubPlanRetention()
		);

		lifecycle.manage(workers);

		environment.admin().addTask(new LoadStorageTask(getName(), null, workers));

		clusterConnection =
				new ClusterConnectionShard(config, environment, workers, internalMapperFactory);

		lifecycle.manage(clusterConnection);

		final Collection<? extends WorkerStorage> workerStorages = config.getStorage().discoverWorkerStorages();


		ExecutorService loaders = config.getQueries().getExecutionPool().createService("Worker loader");

		Queue<Worker> workersDone = new ConcurrentLinkedQueue<>();
		for (WorkerStorage workerStorage : workerStorages) {
			loaders.submit(() -> {
				try {
					workersDone.add(workers.openWorker(workerStorage, config.isFailOnError(), config.getStorage().isLoadStoresOnStart()));
				}
				catch (Exception e) {
					log.error("Failed reading Storage", e);
				}
				finally {
					log.debug("DONE reading Storage {}", workerStorage);
					ConqueryMDC.clearLocation();
				}
			});
		}

		loaders.shutdown();
		while (!loaders.awaitTermination(1, TimeUnit.MINUTES)) {

			log.debug("Waiting for Worker workers to load. {} are already finished. {} pending", workersDone.size(), workerStorages.size()
																													 - workersDone.size());
		}

		log.info("All Worker loaded: {}", workers.getWorkers().size());
	}



	public boolean isBusy() {
		return clusterConnection.isBusy() || workers.isBusy();
	}
}

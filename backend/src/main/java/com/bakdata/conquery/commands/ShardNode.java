package com.bakdata.conquery.commands;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.validation.Validator;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.mode.cluster.ClusterConnectionShard;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.models.worker.Workers;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.Managed;
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
public class ShardNode implements ConfiguredBundle<ConqueryConfig>, Managed {

	public static final String DEFAULT_NAME = "shard-node";

	private final String name;
	private JobManager jobManager;
	@Setter
	private Workers workers;
	private ClusterConnectionShard clusterConnection;

	public ShardNode() {
		this(DEFAULT_NAME);
	}

	public ShardNode(String name) {
		this.name = name;
	}


	@Override
	public void run(ConqueryConfig config, Environment environment) throws Exception {

		jobManager = new JobManager(getName(), config.isFailOnError());
		environment.lifecycle().manage(this);

		workers = new Workers(
				config.getQueries().getExecutionPool(),
				() -> createInternalObjectMapper(View.Persistence.Shard.class, config, environment.getValidator()),
				() -> createInternalObjectMapper(View.InternalCommunication.class, config, environment.getValidator()),
				config.getCluster().getEntityBucketSize(),
				config.getQueries().getSecondaryIdSubPlanRetention()
		);


		clusterConnection =
				new ClusterConnectionShard(config, environment, workers, jobManager, () -> createInternalObjectMapper(View.InternalCommunication.class, config, environment.getValidator()));

		environment.lifecycle().manage(clusterConnection);

		final Collection<WorkerStorage> workerStorages = config.getStorage().discoverWorkerStorages();


		ExecutorService loaders = config.getQueries().getExecutionPool().createService("Worker loader");

		Queue<Worker> workersDone = new ConcurrentLinkedQueue<>();
		for (WorkerStorage workerStorage : workerStorages) {
			loaders.submit(() -> {
				try {
					workersDone.add(workers.createWorker(workerStorage, config.isFailOnError()));
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

	/**
	 * Pendant to {@link ManagerNode#createInternalObjectMapper(Class)}.
	 * <p>
	 * TODO May move to {@link ConqueryCommand}
	 *
	 * @return a preconfigured binary object mapper
	 */
	private static ObjectMapper createInternalObjectMapper(Class<? extends View> viewClass, ConqueryConfig config, Validator validator) {
		final ObjectMapper objectMapper = config.configureObjectMapper(Jackson.copyMapperAndInjectables(Jackson.BINARY_MAPPER));

		final MutableInjectableValues injectableValues = new MutableInjectableValues();
		objectMapper.setInjectableValues(injectableValues);
		injectableValues.add(Validator.class, validator);


		// Set serialization config
		SerializationConfig serializationConfig = objectMapper.getSerializationConfig();

		serializationConfig = serializationConfig.withView(viewClass);

		objectMapper.setConfig(serializationConfig);

		// Set deserialization config
		DeserializationConfig deserializationConfig = objectMapper.getDeserializationConfig();

		deserializationConfig = deserializationConfig.withView(viewClass);

		objectMapper.setConfig(deserializationConfig);

		return objectMapper;
	}

	@Override
	public void start() throws Exception {
		for (Worker value : workers.getWorkers().values()) {
			value.getJobManager().addSlowJob(new SimpleJob("Update Bucket Manager", value.getBucketManager()::fullUpdate));
		}


	}

	@Override
	public void stop() throws Exception {
		getJobManager().close();

		workers.stop();
	}

	public boolean isBusy() {
		return getJobManager().isSlowWorkerBusy() || workers.isBusy();
	}
}

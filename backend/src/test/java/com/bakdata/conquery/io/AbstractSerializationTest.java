package com.bakdata.conquery.io;

import jakarta.validation.Validator;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.WorkerStorageImpl;
import com.bakdata.conquery.mode.cluster.ClusterNamespaceHandler;
import com.bakdata.conquery.mode.cluster.ClusterState;
import com.bakdata.conquery.mode.cluster.InternalMapperFactory;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.ShardWorkers;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jersey.validation.Validators;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;

@Getter
public abstract class AbstractSerializationTest {

	private final Validator validator = Validators.newValidator();
	private final ConqueryConfig config = new ConqueryConfig();
	private DatasetRegistry<DistributedNamespace> datasetRegistry;
	private NamespaceStorage namespaceStorage;
	private MetaStorage metaStorage;
	private WorkerStorageImpl workerStorage;

	private ObjectMapper managerInternalMapper;
	private ObjectMapper namespaceInternalMapper;
	private ObjectMapper shardInternalMapper;
	private ObjectMapper apiMapper;


	@BeforeEach
	public void before() {
		final InternalMapperFactory internalMapperFactory = new InternalMapperFactory(config, validator);
		final IndexService indexService = new IndexService(config.getCsv().createCsvParserSettings(), "emptyDefaultLabel");
		NonPersistentStoreFactory storageFactory = new NonPersistentStoreFactory();
		metaStorage = new MetaStorage(storageFactory);
		namespaceStorage = new NamespaceStorage(storageFactory, "");
		workerStorage = new WorkerStorageImpl(new NonPersistentStoreFactory(), "serializationTestWorker");
		final ClusterNamespaceHandler clusterNamespaceHandler = new ClusterNamespaceHandler(new ClusterState(), config, internalMapperFactory);
		datasetRegistry = new DatasetRegistry<>(0, config, internalMapperFactory, clusterNamespaceHandler, indexService);

		MetricRegistry metricRegistry = new MetricRegistry();

		managerInternalMapper = internalMapperFactory.createManagerPersistenceMapper(datasetRegistry, metaStorage);
		metaStorage.openStores(managerInternalMapper);


		namespaceInternalMapper = internalMapperFactory.createNamespacePersistenceMapper(namespaceStorage);
		namespaceStorage.openStores(namespaceInternalMapper);

		// Prepare worker persistence mapper
		workerStorage.openStores(shardInternalMapper);
		ShardWorkers workers = new ShardWorkers(
				config.getQueries().getExecutionPool(),
				internalMapperFactory,
				config.getCluster().getEntityBucketSize(),
				config.getQueries().getSecondaryIdSubPlanRetention()
		);
		shardInternalMapper = internalMapperFactory.createWorkerPersistenceMapper(workerStorage);

		// Prepare api response mapper
		apiMapper = Jackson.copyMapperAndInjectables(Jackson.MAPPER);
		internalMapperFactory.customizeApiObjectMapper(apiMapper, datasetRegistry, metaStorage);
		// This overrides the injected datasetRegistry
		namespaceStorage.injectInto(apiMapper);
	}


}

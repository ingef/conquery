package com.bakdata.conquery.io;

import jakarta.validation.Validator;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.io.storage.WorkerStorageImpl;
import com.bakdata.conquery.mode.cluster.ClusterNamespaceHandler;
import com.bakdata.conquery.mode.cluster.ClusterState;
import com.bakdata.conquery.mode.cluster.InternalMapperFactory;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.bakdata.conquery.util.TestNamespacedStorageProvider;
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
	private NamespacedStorageProvider shardNamespacedStorageProvider;

	private ObjectMapper managerInternalMapper;
	private ObjectMapper namespacePersistenceMapper;
	private ObjectMapper internalCommunicationMapper;
	private ObjectMapper workerPersistenceMapper;
	private ObjectMapper apiMapper;


	@BeforeEach
	public void before() {
		final InternalMapperFactory internalMapperFactory = new InternalMapperFactory(config, validator);
		final IndexService indexService = new IndexService(config.getCsv().createCsvParserSettings(), "emptyDefaultLabel");
		NonPersistentStoreFactory storageFactory = new NonPersistentStoreFactory();
		metaStorage = storageFactory.createMetaStorage();
		namespaceStorage = storageFactory.createNamespaceStorage();
		workerStorage = storageFactory.createWorkerStorage();

		final ClusterNamespaceHandler clusterNamespaceHandler = new ClusterNamespaceHandler(new ClusterState(), config, internalMapperFactory);
		datasetRegistry = new DatasetRegistry<>(config, internalMapperFactory, clusterNamespaceHandler, indexService) {
			@Override
			public NamespacedStorage getStorage(DatasetId datasetId) {
				return getNamespaceStorage();
			}
		};

		managerInternalMapper = internalMapperFactory.createManagerPersistenceMapper(datasetRegistry, metaStorage);
		metaStorage.openStores(managerInternalMapper);

		namespacePersistenceMapper = internalMapperFactory.createNamespacePersistenceMapper(namespaceStorage, datasetRegistry);
		namespaceStorage.openStores(namespacePersistenceMapper);

		shardNamespacedStorageProvider = new TestNamespacedStorageProvider(getWorkerStorage());

		workerPersistenceMapper = internalMapperFactory.createWorkerPersistenceMapper(workerStorage);
		workerStorage.openStores(workerPersistenceMapper);

		internalCommunicationMapper = internalMapperFactory.createInternalCommunicationMapper(datasetRegistry);

		// Prepare api response mapper
		apiMapper = Jackson.copyMapperAndInjectables(Jackson.MAPPER);
		internalMapperFactory.customizeApiObjectMapper(apiMapper, datasetRegistry, metaStorage);
		// This overrides the injected datasetRegistry
		namespaceStorage.injectInto(apiMapper);
	}


}

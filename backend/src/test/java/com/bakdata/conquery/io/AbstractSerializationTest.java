package com.bakdata.conquery.io;

import static org.mockito.Mockito.mock;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.cluster.ClusterNamespaceHandler;
import com.bakdata.conquery.mode.cluster.ClusterState;
import com.bakdata.conquery.mode.cluster.InternalMapperFactory;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.ShardWorkers;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jersey.validation.Validators;
import jakarta.validation.Validator;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;

@Getter
public abstract class AbstractSerializationTest {

	private final Validator validator = Validators.newValidator();
	private final ConqueryConfig config = new ConqueryConfig();
	private DatasetRegistry<DistributedNamespace> datasetRegistry;
	private MetaStorage metaStorage;
	private NamespaceStorage namespaceStorage;
	private IndexService indexService;


	private ObjectMapper managerInternalMapper;
	private ObjectMapper namespaceInternalMapper;
	private ObjectMapper shardInternalMapper;
	private ObjectMapper apiMapper;

	@BeforeEach
	public void before() {
		final InternalMapperFactory internalMapperFactory = new InternalMapperFactory(config, validator);
		NonPersistentStoreFactory storageFactory = new NonPersistentStoreFactory();
		metaStorage = new MetaStorage(storageFactory);
		namespaceStorage = new NamespaceStorage(storageFactory, "");
		indexService = new IndexService(config.getCsv().createCsvParserSettings(), "emptyDefaultLabel");
		final ClusterNamespaceHandler clusterNamespaceHandler = new ClusterNamespaceHandler(new ClusterState(), config, internalMapperFactory);
		datasetRegistry = new DatasetRegistry<>(0, config, internalMapperFactory, clusterNamespaceHandler, indexService);

		// Prepare manager node internal mapper
		managerInternalMapper = internalMapperFactory.createManagerPersistenceMapper(datasetRegistry, metaStorage);

		metaStorage.openStores(managerInternalMapper);
		metaStorage.loadData();

		// Prepare namespace persistence mapper
		namespaceInternalMapper = internalMapperFactory.createNamespacePersistenceMapper(datasetRegistry);
		namespaceStorage.injectInto(namespaceInternalMapper);
		namespaceStorage.openStores(namespaceInternalMapper);
		namespaceStorage.loadData();
		namespaceStorage.updateDataset(new Dataset("serialization_test"));

		// Prepare shard node internal mapper
		final ShardWorkers workers = mock(ShardWorkers.class);
		shardInternalMapper = internalMapperFactory.createWorkerPersistenceMapper(workers);

		// Prepare api mapper with a Namespace injected (usually done by PathParamInjector)
		apiMapper = Jackson.copyMapperAndInjectables(Jackson.MAPPER);
		internalMapperFactory.customizeApiObjectMapper(apiMapper, datasetRegistry, metaStorage);
		namespaceStorage.injectInto(apiMapper);
	}
}

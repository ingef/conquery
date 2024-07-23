package com.bakdata.conquery.io;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.validation.Validator;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.mode.InternalObjectMapperCreator;
import com.bakdata.conquery.mode.cluster.ClusterNamespaceHandler;
import com.bakdata.conquery.mode.cluster.ClusterState;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.util.extentions.MetaStorageExtension;
import com.bakdata.conquery.util.extentions.NamespaceStorageExtension;
import com.bakdata.conquery.util.extentions.WorkerStorageExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jersey.validation.Validators;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

@Getter
public abstract class AbstractSerializationTest {

	@RegisterExtension
	private static final MetaStorageExtension META_STORAGE_EXTENSION = new MetaStorageExtension();
	@RegisterExtension
	private static final NamespaceStorageExtension NAMESPACE_STORAGE_EXTENSION = new NamespaceStorageExtension();
	@RegisterExtension
	private static final WorkerStorageExtension WORKER_STORAGE_EXTENSION = new WorkerStorageExtension();

	private final Validator validator = Validators.newValidator();
	private final ConqueryConfig config = new ConqueryConfig();
	private DatasetRegistry<DistributedNamespace> datasetRegistry;

	private ObjectMapper managerInternalMapper;
	private ObjectMapper shardInternalMapper;
	private ObjectMapper apiMapper;


	@BeforeEach
	public void before() {
		MetaStorage metaStorage = META_STORAGE_EXTENSION.getMetaStorage();
		InternalObjectMapperCreator creator = new InternalObjectMapperCreator(config, validator);
		final IndexService indexService = new IndexService(config.getCsv().createCsvParserSettings(), "emptyDefaultLabel");
		final ClusterNamespaceHandler clusterNamespaceHandler = new ClusterNamespaceHandler(new ClusterState(), config, creator);
		datasetRegistry = new DatasetRegistry<>(0, config, null, clusterNamespaceHandler, indexService);
		datasetRegistry.setMetaStorage(metaStorage);
		creator.init(datasetRegistry);

		// Prepare manager node internal mapper
		final ManagerNode managerNode = mock(ManagerNode.class);
		when(managerNode.getConfig()).thenReturn(config);
		when(managerNode.getValidator()).thenReturn(validator);
		doReturn(datasetRegistry).when(managerNode).getDatasetRegistry();
		when(managerNode.getStorage()).thenReturn(metaStorage);
		when(managerNode.getInternalObjectMapperCreator()).thenReturn(creator);

		when(managerNode.createInternalObjectMapper(any())).thenCallRealMethod();
		managerInternalMapper = managerNode.createInternalObjectMapper(View.Persistence.Manager.class);

		metaStorage.openStores(managerInternalMapper);
		metaStorage.loadData();

		// Prepare shard node internal mapper
		final ShardNode shardNode = mock(ShardNode.class);
		when(shardNode.getConfig()).thenReturn(config);
		when(shardNode.getValidator()).thenReturn(validator);

		when(shardNode.createInternalObjectMapper(any())).thenCallRealMethod();
		shardInternalMapper = shardNode.createInternalObjectMapper(View.Persistence.Shard.class);

		// Prepare api response mapper
		doCallRealMethod().when(managerNode).customizeApiObjectMapper(any(ObjectMapper.class));
		apiMapper = Jackson.copyMapperAndInjectables(Jackson.MAPPER);
		managerNode.customizeApiObjectMapper(apiMapper);

		// These api injections are usually done by the PathParamInjector/Manager
		getNamespaceStorage().injectInto(apiMapper);
		getMetaStorage().injectInto(apiMapper);

		// These internal injections are usually done by the namespace/worker
		getNamespaceStorage().injectInto(managerInternalMapper);
		getWorkerStorage().injectInto(shardInternalMapper);

	}

	protected MetaStorage getMetaStorage() {
		return META_STORAGE_EXTENSION.getMetaStorage();
	}

	protected NamespaceStorage getNamespaceStorage() {
		return NAMESPACE_STORAGE_EXTENSION.getStorage();
	}

	protected WorkerStorage getWorkerStorage() {
		return WORKER_STORAGE_EXTENSION.getStorage();
	}
}

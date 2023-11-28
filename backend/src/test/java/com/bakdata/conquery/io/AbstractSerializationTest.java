package com.bakdata.conquery.io;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import javax.validation.Validator;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.mode.InternalObjectMapperCreator;
import com.bakdata.conquery.mode.cluster.ClusterNamespaceHandler;
import com.bakdata.conquery.mode.cluster.ClusterState;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jersey.validation.Validators;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;

@Getter
public abstract class AbstractSerializationTest {

	private final Validator validator = Validators.newValidator();
	private final ConqueryConfig config = new ConqueryConfig();
	private DatasetRegistry<DistributedNamespace> datasetRegistry;
	private MetaStorage metaStorage;

	private ObjectMapper managerInternalMapper;
	private ObjectMapper shardInternalMapper;
	private ObjectMapper apiMapper;


	@BeforeEach
	public void before() {
		InternalObjectMapperCreator creator = new InternalObjectMapperCreator(config, validator);
		final IndexService indexService = new IndexService(config.getCsv().createCsvParserSettings());
		final ClusterNamespaceHandler clusterNamespaceHandler = new ClusterNamespaceHandler(new ClusterState(), config, creator);
		datasetRegistry = new DatasetRegistry<>(0, config, null, clusterNamespaceHandler, indexService);
		metaStorage = new MetaStorage(new NonPersistentStoreFactory(), datasetRegistry);
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
	}


}

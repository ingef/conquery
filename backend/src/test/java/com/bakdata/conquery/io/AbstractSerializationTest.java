package com.bakdata.conquery.io;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import jakarta.validation.Validator;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.mode.InternalObjectMapperCreator;
import com.bakdata.conquery.mode.cluster.ClusterNamespaceHandler;
import com.bakdata.conquery.mode.cluster.ClusterState;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jersey.validation.Validators;
import lombok.Getter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

@Getter
public abstract class AbstractSerializationTest {

	private final Validator validator = Validators.newValidator();
	private final ConqueryConfig config = new ConqueryConfig() {{
		this.setStorage(new NonPersistentStoreFactory());
	}};
	private DatasetRegistry<DistributedNamespace> datasetRegistry;
	private Namespace namespace;
	private MetaStorage metaStorage;


	private ObjectMapper managerMetaInternalMapper;
	private ObjectMapper namespaceInternalMapper;
	private ObjectMapper shardInternalMapper;
	private ObjectMapper apiMapper;

	@BeforeAll
	public static void beforeAll() {
		// Some components need the shared registry, and it might have been set already by another test
		if (SharedMetricRegistries.tryGetDefault() == null) {
			SharedMetricRegistries.setDefault(AbstractSerializationTest.class.getSimpleName());
		}
	}


	@BeforeEach
	public void before() throws IOException {

		metaStorage = new MetaStorage(new NonPersistentStoreFactory());
		InternalObjectMapperCreator creator = new InternalObjectMapperCreator(config, metaStorage, validator);
		final IndexService indexService = new IndexService(config.getCsv().createCsvParserSettings(), "emptyDefaultLabel");
		final ClusterNamespaceHandler clusterNamespaceHandler = new ClusterNamespaceHandler(new ClusterState(), config, creator);
		datasetRegistry = new DatasetRegistry<>(0, config, creator, clusterNamespaceHandler, indexService);
		creator.init(datasetRegistry);

		namespace = datasetRegistry.createNamespace(new Dataset("serialization_test"), metaStorage, new Environment(this.getClass().getSimpleName()));

		// Prepare manager meta internal mapper
		managerMetaInternalMapper = creator.createInternalObjectMapper(View.Persistence.Manager.class);
		metaStorage.openStores(managerMetaInternalMapper);
		metaStorage.loadData();

		// Prepare namespace internal mapper
		namespaceInternalMapper = creator.createInternalObjectMapper(View.Persistence.Manager.class);
		namespace.getInjectables().forEach(injectable -> injectable.injectInto(namespaceInternalMapper));

		// Prepare shard node internal mapper
		shardInternalMapper = ShardNode.createInternalObjectMapper(View.Persistence.Shard.class, config, validator);

		// Prepare api mapper with a Namespace injected (usually done by PathParamInjector)
		apiMapper = Jackson.copyMapperAndInjectables(Jackson.MAPPER);
		ManagerNode.customizeApiObjectMapper(apiMapper, datasetRegistry, metaStorage, config, validator);
		namespace.getInjectables().forEach(i -> i.injectInto(apiMapper));
	}
}

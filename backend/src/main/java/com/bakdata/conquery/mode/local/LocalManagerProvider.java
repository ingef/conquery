package com.bakdata.conquery.mode.local;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.bakdata.conquery.mode.DelegateManager;
import com.bakdata.conquery.mode.InternalObjectMapperCreator;
import com.bakdata.conquery.mode.ManagerProvider;
import com.bakdata.conquery.mode.NamespaceHandler;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.LocalNamespace;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import io.dropwizard.setup.Environment;

public class LocalManagerProvider implements ManagerProvider {

	private static final Supplier<Collection<ShardNodeInformation>> EMPTY_NODE_PROVIDER = Collections::emptyList;

	public DelegateManager<LocalNamespace> provideManager(ConqueryConfig config, Environment environment) {
		InternalObjectMapperCreator creator = ManagerProvider.newInternalObjectMapperCreator(config, environment.getValidator());
		NamespaceHandler<LocalNamespace> namespaceHandler = new LocalNamespaceHandler(config, creator);
		DatasetRegistry<LocalNamespace> datasetRegistry = ManagerProvider.createDatasetRegistry(namespaceHandler, config, creator);
		creator.init(datasetRegistry);

		return new DelegateManager<>(
				config,
				environment,
				datasetRegistry,
				new FailingImportHandler(),
				new LocalStorageListener(),
				EMPTY_NODE_PROVIDER,
				List.of(),
				creator,
				ManagerProvider.newJobManager(config)
		);
	}
}

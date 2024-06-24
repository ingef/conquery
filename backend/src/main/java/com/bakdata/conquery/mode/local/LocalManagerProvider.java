package com.bakdata.conquery.mode.local;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.mode.DelegateManager;
import com.bakdata.conquery.mode.InternalObjectMapperCreator;
import com.bakdata.conquery.mode.ManagerProvider;
import com.bakdata.conquery.mode.NamespaceHandler;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.LocalNamespace;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialectFactory;
import io.dropwizard.core.setup.Environment;

public class LocalManagerProvider implements ManagerProvider {

	private static final Supplier<Collection<ShardNodeInformation>> EMPTY_NODE_PROVIDER = Collections::emptyList;

	private final SqlDialectFactory dialectFactory;

	public LocalManagerProvider() {
		this.dialectFactory = new SqlDialectFactory();
	}

	public LocalManagerProvider(SqlDialectFactory dialectFactory) {
		this.dialectFactory = dialectFactory;
	}

	public DelegateManager<LocalNamespace> provideManager(ConqueryConfig config, Environment environment) {

		InternalObjectMapperCreator creator = ManagerProvider.newInternalObjectMapperCreator(config, environment.getValidator());
		NamespaceHandler<LocalNamespace> namespaceHandler = new LocalNamespaceHandler(config, creator, dialectFactory);
		DatasetRegistry<LocalNamespace> datasetRegistry = ManagerProvider.createDatasetRegistry(namespaceHandler, config, creator);
		final MetaStorage metaStorage = ManagerProvider.createMetaStorage(config.getStorage());
		creator.init(datasetRegistry, metaStorage);

		return new DelegateManager<>(
				config,
				environment,
				datasetRegistry,
				metaStorage,
				new FailingImportHandler(),
				new LocalStorageListener(),
				EMPTY_NODE_PROVIDER,
				List.of(),
				creator,
				ManagerProvider.newJobManager(config)
		);
	}

}

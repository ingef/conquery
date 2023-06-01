package com.bakdata.conquery.integration.sql;

import javax.validation.Validator;

import com.bakdata.conquery.integration.IntegrationTests;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.InternalObjectMapperCreator;
import com.bakdata.conquery.mode.local.LocalNamespaceHandler;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.LocalNamespace;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.bakdata.conquery.util.support.TestSupport;
import io.dropwizard.jersey.validation.Validators;
import lombok.Value;

@Value
public class SqlStandaloneSupport implements TestSupport {

	private static final Validator VALIDATOR = Validators.newValidator();
	Dataset dataset;
	Namespace namespace;
	ConqueryConfig config;
	MetaStorage metaStorage;


	public SqlStandaloneSupport() {
		this.dataset = new Dataset("test");
		NamespaceStorage storage = new NamespaceStorage(new NonPersistentStoreFactory(), "", VALIDATOR) {
		};
		storage.openStores(Jackson.MAPPER.copy());
		storage.updateDataset(dataset);

		config = IntegrationTests.DEFAULT_CONFIG;
		InternalObjectMapperCreator mapperCreator = new InternalObjectMapperCreator(config, getValidator());
		LocalNamespaceHandler localNamespaceHandler = new LocalNamespaceHandler(config, mapperCreator);
		DatasetRegistry<LocalNamespace> datasetRegistry = new DatasetRegistry<>(2, config, mapperCreator, localNamespaceHandler);
		metaStorage = new MetaStorage(new NonPersistentStoreFactory(), datasetRegistry);
		datasetRegistry.setMetaStorage(metaStorage);
		mapperCreator.init(datasetRegistry);
		namespace = localNamespaceHandler.createNamespace(storage, metaStorage);
	}

	@Override
	public Namespace getNamespace() {
		return namespace;
	}

	@Override
	public Validator getValidator() {
		return VALIDATOR;
	}

	@Override
	public MetaStorage getMetaStorage() {
		return metaStorage;
	}

	@Override
	public NamespaceStorage getNamespaceStorage() {
		return namespace.getStorage();
	}

	@Override
	public ConqueryConfig getConfig() {
		return config;
	}
}

package com.bakdata.conquery.integration.sql;

import java.util.function.Function;

import javax.validation.Validator;

import com.bakdata.conquery.integration.IntegrationTests;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.bakdata.conquery.util.support.TestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
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
		Function<Class<? extends View>, ObjectMapper> mapperFunction = view -> Jackson.MAPPER.copy();

		this.config = IntegrationTests.DEFAULT_CONFIG;
		this.namespace = Namespace.create(null, storage, IntegrationTests.DEFAULT_CONFIG, mapperFunction);
		this.metaStorage = new MetaStorage(new NonPersistentStoreFactory(), new DatasetRegistry(2, this.config, mapperFunction));
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

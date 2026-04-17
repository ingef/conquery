package com.bakdata.conquery.quarkus.storage.meta;

import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import com.bakdata.conquery.quarkus.storage.FormConfigRepository;
import com.bakdata.conquery.quarkus.storage.QueryRepository;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@IfBuildProperty(name = "conquery.storage.backend", stringValue = "IN_MEMORY", enableIfMissing = true)
public class InMemoryMetaStorageFacade implements ManagerMetaStorage {

	@Inject
	DatasetCatalogRepository datasetCatalogRepository;

	@Inject
	QueryRepository queryRepository;

	@Inject
	FormConfigRepository formConfigRepository;

	@Override
	public DatasetCatalogRepository datasets() {
		return datasetCatalogRepository;
	}

	@Override
	public QueryRepository queries() {
		return queryRepository;
	}

	@Override
	public FormConfigRepository formConfigs() {
		return formConfigRepository;
	}
}

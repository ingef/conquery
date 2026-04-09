package com.bakdata.conquery.quarkus.storage.meta;

import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import com.bakdata.conquery.quarkus.storage.FormConfigRepository;
import com.bakdata.conquery.quarkus.storage.QueryRepository;

public interface ManagerMetaStorage {

	DatasetCatalogRepository datasets();

	QueryRepository queries();

	FormConfigRepository formConfigs();
}

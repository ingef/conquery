package com.bakdata.conquery.quarkus.storage.meta;

import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import com.bakdata.conquery.quarkus.storage.FormConfigRepository;
import com.bakdata.conquery.quarkus.storage.QueryRepository;

/**
 * Integration seam for a future Xodus-backed manager meta storage implementation.
 */
public class XodusMetaStorageFacade implements ManagerMetaStorage {

	private final DatasetCatalogRepository datasetCatalogRepository;
	private final QueryRepository queryRepository;
	private final FormConfigRepository formConfigRepository;

	public XodusMetaStorageFacade(
			DatasetCatalogRepository datasetCatalogRepository,
			QueryRepository queryRepository,
			FormConfigRepository formConfigRepository
	) {
		this.datasetCatalogRepository = datasetCatalogRepository;
		this.queryRepository = queryRepository;
		this.formConfigRepository = formConfigRepository;
	}

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

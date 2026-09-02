package com.bakdata.conquery.quarkus.storage.meta;

import com.bakdata.conquery.quarkus.storage.FormConfigRepository;
import com.bakdata.conquery.quarkus.storage.QueryRepository;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Integration seam for a future Xodus-backed manager meta storage implementation.
 */
@ApplicationScoped
@IfBuildProperty(name = "conquery.storage.backend", stringValue = "XODUS")
public class XodusMetaStorageFacade implements ManagerMetaStorage {

	private final QueryRepository queryRepository;
	private final FormConfigRepository formConfigRepository;

	@Inject
	public XodusMetaStorageFacade(
			QueryRepository queryRepository,
			FormConfigRepository formConfigRepository
	) {
		this.queryRepository = queryRepository;
		this.formConfigRepository = formConfigRepository;
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

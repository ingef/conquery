package com.bakdata.conquery.quarkus.storage.meta;

import com.bakdata.conquery.quarkus.storage.FormConfigRepository;
import com.bakdata.conquery.quarkus.storage.QueryRepository;

public interface ManagerMetaStorage {

	QueryRepository queries();

	FormConfigRepository formConfigs();
}

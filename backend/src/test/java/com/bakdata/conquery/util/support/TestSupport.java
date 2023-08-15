package com.bakdata.conquery.util.support;

import javax.validation.Validator;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.worker.Namespace;

public interface TestSupport {

	Dataset getDataset();

	Namespace getNamespace();

	Validator getValidator();

	MetaStorage getMetaStorage();

	NamespaceStorage getNamespaceStorage();

	ConqueryConfig getConfig();

	User getTestUser();

}

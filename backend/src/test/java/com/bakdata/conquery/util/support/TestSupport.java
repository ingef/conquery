package com.bakdata.conquery.util.support;

import jakarta.validation.Validator;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.UriBuilder;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;

public interface TestSupport {

	DatasetRegistry<?> getDatasetRegistry();

	DatasetId getDataset();

	Namespace getNamespace();

	Validator getValidator();

	MetaStorage getMetaStorage();

	NamespaceStorage getNamespaceStorage();

	ConqueryConfig getConfig();

	User getTestUser();

	AuthorizationController getAuthorizationController();

	Client getClient();

	UriBuilder defaultApiURIBuilder();

}

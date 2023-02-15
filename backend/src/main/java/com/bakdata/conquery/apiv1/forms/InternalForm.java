package com.bakdata.conquery.apiv1.forms;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespace;

public interface InternalForm {

	Map<String, List<ManagedQuery>> createSubQueries(Namespace namespace, User user, MetaStorage storage);

}

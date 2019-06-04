package com.bakdata.conquery.apiv1;

import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QueryProcessor {

	private final Namespaces namespaces;
	private final MasterMetaStorage storage;

	public ExecutionStatus postQuery(Dataset dataset, IQuery query, URLBuilder urlb, User user) throws JSONException {
		Namespace namespace = namespaces.get(dataset.getId());
		
		ManagedQuery mq = namespace.getQueryManager().createQuery(query, user);
		
		// Set abilities for submitted query
		user.addPermission(storage, new QueryPermission(null, AbilitySets.QUERY_CREATOR, mq.getId()));
		mq.awaitDone(10, TimeUnit.SECONDS);
		
		return mq.buildStatus(urlb);
	}

	public ExecutionStatus getStatus(Dataset dataset, ManagedExecution query, URLBuilder urlb) {
		query.awaitDone(10, TimeUnit.SECONDS);
		
		return query.buildStatus(urlb);
	}

	public ExecutionStatus cancel(Dataset dataset, ManagedExecution query, URLBuilder urlb) {
		
		return null;
	}

}

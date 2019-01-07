package com.bakdata.conquery.apiv1;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;

public class QueryProcessor {

	private final Namespaces namespaces;
	private final ConqueryConfig config;

	public QueryProcessor(Namespaces namespaces, ConqueryConfig config) {
		this.namespaces = namespaces;
		this.config = config;
	}

	public SQStatus postQuery(Dataset dataset, IQuery query, URLBuilder urlb, User user) throws JSONException {
		// TODO AUTH authorizeDataset(user, primaryDataset);
		Namespace namespace = namespaces.get(dataset.getId());
		
		// Check dataset
		user.checkPermission(new DatasetPermission(null, EnumSet.of(Ability.READ), dataset.getId()));
		
		// Check reused query
		for(ManagedQueryId requiredQueryId : query.collectRequiredQueries()) {
			user.checkPermission(new QueryPermission(null, EnumSet.of(Ability.READ), requiredQueryId));
		}
		ManagedQuery mq = namespace.getQueryManager().createQuery(query);
		
		// Set abilities for submitted query
		user.addPermission(new QueryPermission(null, AbilitySets.QUERY_CREATOR.getSet(), mq.getId()));
		mq.awaitDone(1, TimeUnit.HOURS);
		
		return SQStatus.buildFromQuery(mq, urlb, config);
	}

	public SQStatus getStatus(Dataset dataset, ManagedQuery query, URLBuilder urlb) {
		// TODO AUTH authorizeDataset(user, dataset);
		// TODO AUTH authorizeQuery(user, queryId, QueryPermission::canRead);
		query.awaitDone(10, TimeUnit.SECONDS);
		return SQStatus.buildFromQuery(query, urlb, config);
	}

	public SQStatus cancel(User user, Dataset dataset, ManagedQuery query, URLBuilder urlb) {
		// TODO AUTH authorizeDataset(user, dataset);
		// TODO AUTH authorizeQuery(user, queryId, QueryPermission::canRead);
		return null;
	}

}

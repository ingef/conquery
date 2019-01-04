package com.bakdata.conquery.apiv1;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import java.util.concurrent.TimeUnit;

public class QueryProcessor {

	private final Namespaces namespaces;
	private final ConqueryConfig config;

	public QueryProcessor(Namespaces namespaces, ConqueryConfig config) {
		this.namespaces = namespaces;
		this.config = config;
	}

	public SQStatus postQuery(Dataset dataset, IQuery query, URLBuilder urlb) throws JSONException {
		Namespace namespace = namespaces.get(dataset.getId());
		ManagedQuery mq = namespace.getQueryManager().createQuery(query);
		mq.awaitDone(1, TimeUnit.HOURS);
		
		return SQStatus.buildFromQuery(mq, urlb, config);
	}

	public SQStatus getStatus(Dataset dataset, ManagedQuery query, URLBuilder urlb) {
		query.awaitDone(10, TimeUnit.SECONDS);
		return SQStatus.buildFromQuery(query, urlb, config);
	}

	public SQStatus cancel(Dataset dataset, ManagedQuery query, URLBuilder urlb) {
		return null;
	}

}

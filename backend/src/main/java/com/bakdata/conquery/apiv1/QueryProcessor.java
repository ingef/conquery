package com.bakdata.conquery.apiv1;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryTranslator;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class QueryProcessor {

	private final Namespaces namespaces;
	private final MasterMetaStorage storage;

	/**
	 * Create query for all datasets, then submit it for execution it on selected
	 * dataset.
	 * 
	 * @param dataset
	 * @param query
	 * @param urlb
	 * @param user
	 * @param allowDownload
	 * @return
	 * @throws JSONException
	 */
	public ExecutionStatus postQuery(Dataset dataset, IQuery query, URLBuilder urlb, User user) throws JSONException {
		Namespace namespace = namespaces.get(dataset.getId());

		ManagedQuery mq = namespace.getQueryManager().runQuery(query, user);

		// Set abilities for submitted query
		user.addPermission(storage, QueryPermission.onInstance(AbilitySets.QUERY_CREATOR, mq.getId()));

		// translate the query for all other datasets of user and submit it.
		for (Namespace targetNamespace : namespaces.getNamespaces()) {
			if (!user.isPermitted(DatasetPermission.onInstance(Ability.READ.asSet(), targetNamespace.getDataset().getId()))
				|| targetNamespace.getDataset().equals(dataset)) {
				continue;
			}

			// Ensure that user is allowed to read all sub-queries of the actual query.
			if (!query.collectRequiredQueries().stream()
				.allMatch(qid -> user.isPermitted(QueryPermission.onInstance(Ability.READ.asSet(), qid))))
				continue;

			try {
				IQuery translated = QueryTranslator.replaceDataset(namespaces, query, targetNamespace.getDataset().getId());
				final ManagedQuery mqTranslated = targetNamespace.getQueryManager().createQuery(translated, mq.getQueryId(), user);

				user.addPermission(storage, QueryPermission.onInstance(AbilitySets.QUERY_CREATOR, mqTranslated.getId()));
			}
			catch (Exception e) {
				log.trace("Could not translate " + query + " to dataset " + targetNamespace.getDataset(), e);
			}
		}

		// return status
		return getStatus(dataset, mq, urlb, user.isPermitted(DatasetPermission.onInstance(Ability.DOWNLOAD, dataset.getId())));
	}

	public ExecutionStatus getStatus(Dataset dataset, ManagedExecution query, URLBuilder urlb, boolean allowDownload) {
		return query.buildStatus(urlb, allowDownload);
	}

	public ExecutionStatus cancel(Dataset dataset, ManagedExecution query, URLBuilder urlb) {

		return null;
	}
}

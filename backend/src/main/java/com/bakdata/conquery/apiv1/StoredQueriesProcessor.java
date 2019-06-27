package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.addPermission;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.removePermission;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.worker.Namespaces;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StoredQueriesProcessor {
	@Getter
	private final Namespaces namespaces;

	public StoredQueriesProcessor(Namespaces namespaces) {
		this.namespaces = namespaces;
	}

	public List<ExecutionStatus> getAllQueries(Dataset dataset, HttpServletRequest req) {
		Collection<ManagedExecution> allQueries = namespaces.get(dataset.getId()).getStorage().getMetaStorage().getAllExecutions();

		return allQueries
			.stream()
			.filter(q -> q.getState() == ExecutionState.DONE)
			//to exclude subtypes from somewhere else
			.filter(q -> (q instanceof ManagedQuery) && ((ManagedQuery)q).getQuery().getClass().equals(ConceptQuery.class))
			.map(mq -> mq.buildStatus(URLBuilder.fromRequest(req)))
			.collect(Collectors.toList());
	}

	public void deleteQuery(Dataset dataset, ManagedExecution query) {
		MasterMetaStorage storage = namespaces.get(dataset.getId()).getStorage().getMetaStorage();
		storage.removeExecution(query.getId());
	}

	public void shareQuery(MasterMetaStorage storage, User user, ManagedQuery query, boolean shared) {
		authorize(user, query, Ability.SHARE);
		QueryPermission queryPermission = new QueryPermission(AbilitySets.QUERY_EXECUTOR, query.getId());
		user.getRoles().forEach((Mandator mandator) -> {
			try {
				if (shared) {
					addPermission(mandator, queryPermission, storage);
				}
				else {
					removePermission(mandator, queryPermission, storage);
				}
				query.setShared(shared);
				storage.updateExecution(query);
			} catch (JSONException e) {
				log.error("", e);
			}
		});
	}

	public void updateQueryLabel(MasterMetaStorage storage, User user, ManagedQuery query, String label) throws JSONException {
		authorize(user, query, Ability.LABEL);
		query.setLabel(label);
		storage.updateExecution(query);
	}

	public void tagQuery(MasterMetaStorage storage, User user, ManagedQuery query, String[] newTags) throws JSONException {
		authorize(user, query, Ability.TAG);
		query.setTags(newTags);
		storage.updateExecution(query);
	}

	public ExecutionStatus getQueryWithSource(Dataset dataset, ManagedExecutionId queryId) {
		ManagedExecution query = namespaces.getMetaStorage().getExecution(queryId);
		if(query == null) {
			return null;
		}
		return query.buildStatus();
	}

}

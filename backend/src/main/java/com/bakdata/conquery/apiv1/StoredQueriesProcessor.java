package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.addPermission;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;
import static com.bakdata.conquery.models.auth.AuthorizationHelper.removePermission;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.worker.Namespace;
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

	public Stream<ExecutionStatus> getAllQueries(Dataset dataset, HttpServletRequest req) {
		Collection<ManagedExecution> allQueries = namespaces.getMetaStorage().getAllExecutions();

		return allQueries
			.stream()
			//to exclude subtypes from somewhere else
			.filter(q -> (q instanceof ManagedQuery) && ((ManagedQuery)q).getQuery().getClass().equals(ConceptQuery.class))
			.filter(q -> q.getDataset().equals(dataset.getId()))
			.flatMap(mq -> {
				try {
					return Stream.of(mq.buildStatus(URLBuilder.fromRequest(req)));
				}
				catch(Exception e) {
					log.warn("Could not build status of "+mq, e);
					return Stream.empty();
				}
			});
	}

	public void deleteQuery(Dataset dataset, ManagedExecution query) {
		MasterMetaStorage storage = namespaces.get(dataset.getId()).getStorage().getMetaStorage();
		storage.removeExecution(query.getId());
	}

	public void shareQuery(User user, ManagedQuery query, boolean shared) throws JSONException {
		updateQueryVersions(user, query, Ability.SHARE, q-> {
			ConqueryPermission queryPermission = QueryPermission.onInstance(AbilitySets.QUERY_EXECUTOR, q.getId());
			user.getRoles().forEach(role -> {
				try {
					if (shared) {
						addPermission(role, queryPermission, namespaces.getMetaStorage());
					}
					else {
						removePermission(role, queryPermission, namespaces.getMetaStorage());
					}
					q.setShared(shared);
					namespaces.getMetaStorage().updateExecution(q);
				} catch (JSONException e) {
					log.error("Failed to set shared status for query "+query, e);
				}
			});
		});
		
	}

	public void updateQueryLabel(User user, ManagedQuery query, String label) throws JSONException {
		updateQueryVersions(user, query, Ability.LABEL, q->q.setLabel(label));
	}

	public void tagQuery(User user, ManagedQuery query, String[] newTags) throws JSONException {
		updateQueryVersions(user, query, Ability.TAG, q->q.setTags(newTags));
	}
	
	public void updateQueryVersions(User user, ManagedQuery query, Ability requiredAbility, Consumer<ManagedQuery> updater) throws JSONException {
		authorize(user, query, requiredAbility);
		
		for(Namespace ns : namespaces.getNamespaces()) {
			if(user.isPermitted(DatasetPermission.onInstance(Ability.READ.asSet(), ns.getDataset().getId()))) {
				ManagedExecutionId id = new ManagedExecutionId(ns.getDataset().getId(), query.getQueryId());
				ManagedQuery exec = (ManagedQuery)namespaces.getMetaStorage().getExecution(id);
				if(exec != null) {
					if(user.isPermitted(QueryPermission.onInstance(requiredAbility.asSet(), id))) {
						updater.accept(exec);
						namespaces.getMetaStorage().updateExecution(exec);
					}
				}
			}
		}
	}

	public ExecutionStatus getQueryWithSource(Dataset dataset, ManagedExecutionId queryId) {
		ManagedExecution query = namespaces.getMetaStorage().getExecution(queryId);
		if(query == null) {
			return null;
		}
		return query.buildStatus();
	}

}

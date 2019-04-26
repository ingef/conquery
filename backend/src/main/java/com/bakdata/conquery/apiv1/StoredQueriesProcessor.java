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
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryStatus;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterators;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StoredQueriesProcessor {
	@Getter
	private final Namespaces namespaces;

	public StoredQueriesProcessor(Namespaces namespaces) {
		this.namespaces = namespaces;
	}

	public List<SQStatus> getAllQueries(Dataset dataset, HttpServletRequest req) {
		Collection<ManagedQuery> allQueries = namespaces.get(dataset.getId()).getStorage().getMetaStorage().getAllQueries();

		return allQueries
			.stream()
			.filter(q -> q.getStatus() == QueryStatus.DONE)
			//to exclude subtypes from somewhere else
			.filter(q -> q.getQuery().getClass().equals(ConceptQuery.class))
			.map(mq -> SQStatus.buildFromQuery(namespaces.getMetaStorage(), mq, URLBuilder.fromRequest(req)))
			.collect(Collectors.toList());
	}

	public void deleteQuery(Dataset dataset, ManagedQuery query) {
		MasterMetaStorage storage = namespaces.get(dataset.getId()).getStorage().getMetaStorage();
		storage.removeQuery(query.getId());
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
				storage.updateQuery(query);
			} catch (JSONException e) {
				log.error("", e);
			}
		});
	}

	public void updateQueryLabel(MasterMetaStorage storage, User user, ManagedQuery query, String label) throws JSONException {
		authorize(user, query, Ability.LABEL);
		query.setLabel(label);
		storage.updateQuery(query);
	}

	public void tagQuery(MasterMetaStorage storage, User user, ManagedQuery query, String[] newTags) throws JSONException {
		authorize(user, query, Ability.TAG);
		query.setTags(newTags);
		storage.updateQuery(query);
	}

	public SQStatus getQueryWithSource(Dataset dataset, ManagedQueryId queryId) {
		ManagedQuery query = namespaces.get(dataset.getId()).getStorage().getMetaStorage().getQuery(queryId);

		return SQStatus.buildFromQuery(namespaces.getMetaStorage(), query);
	}

}

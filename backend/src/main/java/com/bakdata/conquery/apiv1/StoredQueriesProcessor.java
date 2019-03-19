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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StoredQueriesProcessor {

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

	public void patchQuery(User user, Dataset dataset, ManagedQueryId queryId, JsonNode patch) throws JSONException {
		MasterMetaStorage storage = namespaces.get(dataset.getId()).getStorage().getMetaStorage();
		ManagedQuery query = storage.getQuery(queryId);
		if (patch.has("tags")) {
			authorize(user, queryId, Ability.TAG);
			String[] newTags = Iterators.toArray(Iterators.transform(patch.get("tags").elements(), n -> n.asText(null)), String.class);
			query.setTags(newTags);
			storage.updateQuery(query);
		} else if (patch.has("label")) {
			authorize(user, queryId, Ability.LABEL);
			query.setLabel(patch.get("label").textValue());
			storage.updateQuery(query);
		} else if (patch.has("shared")) {
			authorize(user, queryId, Ability.SHARE);
			QueryPermission queryPermission = new QueryPermission(AbilitySets.QUERY_EXECUTOR, queryId);
			boolean shared = patch.get("shared").asBoolean();
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
	}

	public SQStatus getQueryWithSource(Dataset dataset, ManagedQueryId queryId) {
		ManagedQuery query = namespaces.get(dataset.getId()).getStorage().getMetaStorage().getQuery(queryId);

		return SQStatus.buildFromQuery(namespaces.getMetaStorage(), query);
	}

}

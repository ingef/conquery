package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;

import java.util.List;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import com.fasterxml.jackson.databind.JsonNode;

import jersey.repackaged.com.google.common.collect.Iterators;

public class StoredQueriesProcessor {

	private final Namespaces namespaces;

	public StoredQueriesProcessor(Namespaces namespaces) {
		this.namespaces = namespaces;
	}

	public List<SQStatus> getAllQueries(User user, Dataset dataset, URLBuilder fromRequest) {
		// Auto-generated method stub
		return null;
	}

	public void deleteQuery(Dataset dataset, ManagedQuery query) {
		// Auto-generated method stub

	}

	public SQStatus patchQuery(User user, Dataset dataset, ManagedQueryId queryId, JsonNode patch) {


		if (patch.has("tags")) {
			authorize(user, queryId, Ability.TAG);
			String[] newTags = Iterators.toArray(Iterators.transform(patch.get("tags").elements(), n -> n.asText(null)),
					String.class);
			// meta.updateQueryTags(queryId, newTags);
		} else if (patch.has("label")) {
			authorize(user, queryId, Ability.LABEL);
			// meta.updateQueryLabel(queryId, patch.get("label").asText());
		} else if (patch.has("shared")) {
			authorize(user, queryId, Ability.SHARE);
			// meta.updateQueryShared(queryId, patch.get("shared").asBoolean());
		} else
			return null;

		// SQStatus status = meta.getQueryStatus(queryId, dataset);
		// status.setOwn(user.getId() == status.getOwner());
		// return status;
		return null;
	}

	public SQStatus getQueryWithSource(Dataset dataset, ManagedQueryId queryId) {
		// fill body
		return null;
	}

}

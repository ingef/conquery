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
		authorize(user, dataset.getId(), Ability.READ);
		// TODO Auto-generated method stub
		return null;
	}

	public void deleteQuery(User user, Dataset dataset, ManagedQuery query) {
		authorize(user, dataset.getId(), Ability.READ);
		authorize(user, query.getId(), Ability.DELETE);
		// TODO Auto-generated method stub

	}

	public SQStatus patchQuery(User user, Dataset dataset, ManagedQueryId queryId, JsonNode patch) {

		authorize(user, dataset.getId(), Ability.READ);

		if (patch.has("tags")) {
			authorize(user, queryId, Ability.TAG);
			String[] newTags = Iterators.toArray(Iterators.transform(patch.get("tags").elements(), n -> n.asText(null)),
					String.class);
			// TODO meta.updateQueryTags(queryId, newTags);
		} else if (patch.has("label")) {
			authorize(user, queryId, Ability.LABEL);
			// TODO meta.updateQueryLabel(queryId, patch.get("label").asText());
		} else if (patch.has("shared")) {
			authorize(user, queryId, Ability.SHARE);
			// TODO meta.updateQueryShared(queryId, patch.get("shared").asBoolean());
		} else
			return null;

		// TODO SQStatus status = meta.getQueryStatus(queryId, dataset);
		// TODO status.setOwn(user.getId() == status.getOwner());
		// TODO return status;
		return null;
	}

	public SQStatus getQueryWithSource(User user, Dataset dataset, ManagedQueryId queryId) {
		authorize(user, dataset.getId(), Ability.READ);
		authorize(user, queryId, Ability.READ);
		// TODO fill body
		return null;
	}

}

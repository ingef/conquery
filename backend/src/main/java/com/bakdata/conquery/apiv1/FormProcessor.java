package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;

import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.query.ManagedQuery;

public class FormProcessor {

	public SQStatus get(User user, Dataset dataset, ManagedQuery query, URLBuilder fromRequest) {
		authorize(user, dataset.getId(), Ability.READ);
		return null;
	}

	public void cancel(User user, Dataset dataset, ManagedQuery query) {
		authorize(user, dataset.getId(), Ability.READ);
	}
}

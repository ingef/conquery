package com.bakdata.conquery.apiv1.forms;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bakdata.conquery.apiv1.SubmittedQuery;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * API representation of a form query.
 */
@Getter
@Setter
public abstract class Form implements SubmittedQuery {

	@NonNull
	@JsonInclude
	private UUID id = UUID.randomUUID();

	public abstract void init(Namespaces namespaces, User user);
	

	public abstract Map<String, List<ManagedQuery>> createSubQueries(Namespaces namespaces, UserId userId, DatasetId submittedDataset);
	
}

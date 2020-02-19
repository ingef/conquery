package com.bakdata.conquery.apiv1.forms;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.Namespaces;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "type")
@CPSBase
@Getter
@Setter
public abstract class Form implements Visitable {

	@NonNull
	@JsonInclude
	private UUID id = UUID.randomUUID();

	@JsonIgnore
	protected String[] getAdditionalHeader() {
		return new String[0];
	}

	public abstract Map<String, List<ManagedQuery>> executeQuery(Dataset dataset, User user, Namespaces namespaces) throws JSONException, IOException;

	@JsonIgnore
	public abstract Collection<ManagedExecutionId> getUsedQueries();
	
	public abstract void init(Namespaces namespaces, User user);
	
}

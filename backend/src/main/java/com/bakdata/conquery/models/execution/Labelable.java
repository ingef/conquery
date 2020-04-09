package com.bakdata.conquery.models.execution;

import java.util.function.Consumer;

import com.bakdata.conquery.resources.api.StoredQueriesResource.QueryPatch;

public interface Labelable {
	void setLabel(String label);
	
	default Consumer<QueryPatch> labeler() {
		return (patch) -> {setLabel(patch.getLabel());};
		
	}
}

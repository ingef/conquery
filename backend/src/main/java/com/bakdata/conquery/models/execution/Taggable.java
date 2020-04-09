package com.bakdata.conquery.models.execution;

import java.util.function.Consumer;

import com.bakdata.conquery.resources.api.StoredQueriesResource.QueryPatch;

public interface Taggable {
	void setTags(String [] tags);
	
	default Consumer<QueryPatch> tagger() {
		return (patch) -> {setTags(patch.getTags());};
		
	}
}

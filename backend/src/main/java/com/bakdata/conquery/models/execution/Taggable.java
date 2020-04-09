package com.bakdata.conquery.models.execution;

import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.MetaDataPatch;

public interface Taggable {
	void setTags(String [] tags);
	
	default Consumer<MetaDataPatch> tagger() {
		return (patch) -> {setTags(patch.getTags());};
		
	}
}

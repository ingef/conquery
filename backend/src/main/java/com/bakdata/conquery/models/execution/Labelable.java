package com.bakdata.conquery.models.execution;

import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.MetaDataPatch;

public interface Labelable {
	void setLabel(String label);
	
	default Consumer<MetaDataPatch> labeler() {
		return (patch) -> {setLabel(patch.getLabel());};
		
	}
}

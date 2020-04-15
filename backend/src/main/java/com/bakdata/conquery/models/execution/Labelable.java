package com.bakdata.conquery.models.execution;

import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.MetaDataPatch;

/**
 * Interface for classes that are able to be patched with an {@link MetaDataPatch}.
 * Lets the implementing class set the label if the label in the patch is not {@code null}.
 */
public interface Labelable {
	String getLabel();
	void setLabel(String label);
	
	default Consumer<Labelable> labeler() {
		return (patch) -> {
			if (patch != null && patch.getLabel() != null) {
				setLabel(patch.getLabel());
			}
		};		
	}
}

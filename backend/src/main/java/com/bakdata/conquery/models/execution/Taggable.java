package com.bakdata.conquery.models.execution;

import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.MetaDataPatch;

/**
 * Interface for classes that are able to be patched with an
 * {@link MetaDataPatch}. Lets the implementing class set tags if tags in the
 * patch is not {@code null}.
 */
public interface Taggable {

	void setTags(String[] tags);

	default Consumer<MetaDataPatch> tagger() {
		return (patch) -> {
			if (patch != null && patch.getTags() != null) {
				setTags(patch.getTags());
			}
		};

	}
}

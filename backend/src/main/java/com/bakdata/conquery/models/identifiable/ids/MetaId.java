package com.bakdata.conquery.models.identifiable.ids;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NonNull;
import lombok.Setter;

/**
 * Marker interface for Ids that are resolvable in a {@link com.bakdata.conquery.io.storage.MetaStorage}
 */
public abstract class MetaId<T> extends Id<T, MetaStorage> {
	/**
	 * Injected by deserializer for resolving meta Ids
	 */
	@JsonIgnore
	@Setter(onParam_ = {@NonNull})
	private MetaStorage metaStorage;

	@Override
	public MetaStorage getStorage() {
		return metaStorage;
	}
}

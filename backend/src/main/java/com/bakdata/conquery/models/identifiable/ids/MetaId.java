package com.bakdata.conquery.models.identifiable.ids;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.Identifiable;

/**
 * Marker interface for Ids that are resolvable in a {@link com.bakdata.conquery.io.storage.MetaStorage}
 */
public interface MetaId {

	Identifiable<?> get(MetaStorage storage);
}

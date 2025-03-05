package com.bakdata.conquery.io.storage;

import com.bakdata.conquery.io.storage.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.util.functions.ThrowingConsumer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Store for Identifiable values.
 * <p>
 * The {@link ThrowingConsumer}s can be used to reflect/model dependencies of the identifiable values inside the store. For example {@link com.bakdata.conquery.models.datasets.concepts.Concept} holds multiple {@link com.bakdata.conquery.models.datasets.concepts.Connector}s where a deletion of a concept requires the deletion of the Conncetors as well. {@link NamespacedStorageImpl} is the main user of those two methods and should be looked at if desired.
 */
@Accessors(fluent = true)
@Setter
@Getter
public class IdentifiableStore<VALUE extends Identifiable<?>> extends KeyIncludingStore<Id<VALUE>, VALUE> {


	public IdentifiableStore(Store<Id<VALUE>, VALUE> store) {
		super(store);
	}


	@Override
	protected Id<VALUE> extractKey(VALUE value) {
		return (Id<VALUE>) value.getId();
	}
}

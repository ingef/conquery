package com.bakdata.conquery.io.storage;

import com.bakdata.conquery.io.storage.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.util.functions.ThrowingConsumer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent=true) @Setter
@Getter
public abstract class IdentifiableStore<VALUE extends Identifiable<?>> extends KeyIncludingStore<IId<VALUE>, VALUE> {

    protected final CentralRegistry centralRegistry;

    // TODO: 09.01.2020 fk: Consider making these part of a class that is passed on creation instead so they are less loosely bound.
    @NonNull
    protected ThrowingConsumer<VALUE> onAdd = (v) -> {};

    @NonNull
    protected ThrowingConsumer<VALUE> onRemove = (v) -> {};

    public IdentifiableStore(Store<IId<VALUE>, VALUE> store, CentralRegistry centralRegistry) {
        super(store);
        store.inject(centralRegistry);
        this.centralRegistry = centralRegistry;
    }

    @Override
    protected abstract IId<VALUE> extractKey(VALUE value);

    @Override
    protected abstract void removed(VALUE value);

    @Override
    protected abstract void added(VALUE value);
}

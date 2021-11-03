package com.bakdata.conquery.util;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.BiConsumer;

import com.bakdata.conquery.io.storage.Store;
import com.bakdata.conquery.io.storage.xodus.stores.SerializingStore;

public class NonPersistentStore<KEY, VALUE> implements Store<KEY, VALUE> {

    private final HashMap<KEY,VALUE> map = new HashMap<>();

    @Override
    public void add(KEY key, VALUE value) {
        map.put(key, value);
    }

    @Override
    public VALUE get(KEY key) {
        return map.get(key);
    }

    @Override
    public SerializingStore.IterationStatistic forEach(StoreEntryConsumer<KEY, VALUE> consumer) {
        final SerializingStore.IterationStatistic stats = new SerializingStore.IterationStatistic();
        map.forEach(new BiConsumer<KEY, VALUE>() {
            @Override
            public void accept(KEY key, VALUE value) {
                consumer.accept(key,value, stats.getTotalProcessed());
                stats.incrTotalProcessed();

            }
        });
        return stats;
    }

    @Override
    public void update(KEY key, VALUE value) {
        map.put(key,value);
    }

    @Override
    public void remove(KEY key) {
        map.remove(key);
    }

    @Override
    public void fillCache() {

    }

    @Override
    public int count() {
        return map.size();
    }

    @Override
    public Collection<VALUE> getAll() {
        return map.values();
    }

    @Override
    public Collection<KEY> getAllKeys() {
        return map.keySet();
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public void deleteStore() {
        clear();
    }

    @Override
    public void close() throws IOException {
        // Nothing to close
    }
}

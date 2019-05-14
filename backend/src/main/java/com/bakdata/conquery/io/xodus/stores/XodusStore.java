package com.bakdata.conquery.io.xodus.stores;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import com.google.common.primitives.Ints;

import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XodusStore implements Closeable {
	private final Store store;
	private final Environment environment;
	
	public XodusStore(Environment env, IStoreInfo storeId) {
		this.environment = env;
		this.store = env.computeInTransaction(
			t->env.openStore(storeId.getXodusName(), StoreConfig.WITHOUT_DUPLICATES_WITH_PREFIXING, t)
		);
	}
	
	public void add(ByteIterable key, ByteIterable value) {
		environment.executeInTransaction(t -> store.add(t, key, value));
	}

	public ByteIterable get(ByteIterable key) {
		return environment.computeInReadonlyTransaction(t -> store.get(t, key));
	}

	public void forEach(BiConsumer<ByteIterable, ByteIterable> consumer) {
		AtomicReference<ByteIterable> lastKey = new AtomicReference<>();
		environment.executeInReadonlyTransaction(t -> {
			try(Cursor c = store.openCursor(t)) {
				if(!c.getNext()) {
					return;
				}
				lastKey.set(c.getKey());
				consumer.accept(lastKey.get(), c.getValue());
			}
		});
		while(lastKey.get()!=null) {
			environment.executeInReadonlyTransaction(t -> {
				try(Cursor c = store.openCursor(t)) {
					c.getSearchKey(lastKey.get());
					if(!c.getNext()) {
						return;
					}
					lastKey.set(c.getKey());
					consumer.accept(lastKey.get(), c.getValue());
				}
			});
		}
	}

	public void update(ByteIterable key, ByteIterable value) {
		environment.executeInTransaction(t -> store.put(t, key, value));
	}
	
	public void remove(ByteIterable key) {
		environment.executeInTransaction(t -> store.delete(t, key));
	}

	@Override
	public void close() throws IOException {}

	public int count() {
		return Ints.checkedCast(environment.computeInReadonlyTransaction(store::count));
	}
	
	@Override
	public String toString() {
		return store.getName();
	}
}

package com.bakdata.conquery.io.xodus.stores;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.BiConsumer;

import com.bakdata.conquery.io.jackson.serializer.IdReferenceResolvingException;
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
		environment.executeInReadonlyTransaction(t -> {
			try(Cursor c = store.openCursor(t)) {
				while(c.getNext()) {
					try {
						consumer.accept(c.getKey(), c.getValue());
					}
					catch (RuntimeException e) {
						if (e.getCause() instanceof IdReferenceResolvingException) {
							log.warn("Probably failed to read id '{}' because it is not yet present, skipping",  ((IdReferenceResolvingException) e.getCause()).getValue(),e.getCause());
						}
						else {
							throw e;
						}
					}
				}
			}
		});
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

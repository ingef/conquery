package com.bakdata.conquery.io.xodus.stores;

import java.util.concurrent.atomic.AtomicBoolean;
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
public class XodusStore {
	private final Store store;
	private final Environment environment;
	private final long timeoutHalfMillis; // milliseconds
	
	public XodusStore(Environment env, IStoreInfo storeId) {
		// Arbitrary duration that is strictly shorter than the timeout to not get interrupted by StuckTxMonitor
		this.timeoutHalfMillis = env.getEnvironmentConfig().getEnvMonitorTxnsTimeout()/2;
		this.environment = env;
		this.store = env.computeInTransaction(
			t->env.openStore(storeId.getXodusName(), StoreConfig.WITHOUT_DUPLICATES_WITH_PREFIXING, t)
		);
	}
	
	public boolean add(ByteIterable key, ByteIterable value) {
		return environment.computeInTransaction(t -> store.add(t, key, value));
	}

	public ByteIterable get(ByteIterable key) {
		return environment.computeInReadonlyTransaction(t -> store.get(t, key));
	}

	/**
	 * Iterate over all key-value pairs in a consistent manner.
	 * The transaction is read only!
	 * @param consumer function called for-each key-value pair.
	 */
	public void forEach(BiConsumer<ByteIterable, ByteIterable> consumer) {
		AtomicReference<ByteIterable> lastKey = new AtomicReference<>();
		AtomicBoolean done = new AtomicBoolean(false);
		while(!done.get()) {
			environment.executeInReadonlyTransaction(t -> {
				try(Cursor c = store.openCursor(t)) {
					//try to load everything in the same transaction
					//but keep within half of the timeout time
					long start = System.nanoTime();
					//search where we left of
					if(lastKey.get() != null) {
						c.getSearchKey(lastKey.get());
					}
					while(System.nanoTime()-start < timeoutHalfMillis) {
						if(!c.getNext()) {
							done.set(true);
							return;
						}
						lastKey.set(c.getKey());
						consumer.accept(lastKey.get(), c.getValue());
					}
				}
			});
		}
	}

	public boolean update(ByteIterable key, ByteIterable value) {
		return environment.computeInTransaction(t -> store.put(t, key, value));
	}
	
	public boolean remove(ByteIterable key) {
		return environment.computeInTransaction(t -> store.delete(t, key));
	}

	public int count() {
		return Ints.checkedCast(environment.computeInReadonlyTransaction(store::count));
	}
	
	@Override
	public String toString() {
		return store.getName();
	}
}

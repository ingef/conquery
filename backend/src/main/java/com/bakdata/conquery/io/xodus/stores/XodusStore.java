package com.bakdata.conquery.io.xodus.stores;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.BiConsumer;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.google.common.primitives.Ints;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XodusStore implements Closeable {
	// STOPSHIP: 15.07.2020 THIS IS JUST FOR TESTING!
	public static final Collection<Thread> activeThreads = new HashSet<>();

	private final Store store;
	private final Environment environment;
	private final long timeout = ConqueryConfig.getInstance().getStorage().getXodus().getEnvMonitorTxnsTimeout().toNanoseconds()/2;
	
	public XodusStore(Environment env, IStoreInfo storeId) {
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
		ByteIterable lastKey = null;
		activeThreads.add(Thread.currentThread());

		do {
			// Copy lastKey to guarantee it is unchanged.
			final ByteIterable _key = lastKey;
			lastKey = environment.computeInReadonlyTransaction(t -> {

				try (Cursor c = store.openCursor(t)) {
					long start = System.nanoTime();

					// try to load everything in the same transaction
					// but keep within half of the timeout

					// Move cursor to where we left off
					if (_key != null) {
						c.getSearchKey(_key);
					}

					while (System.nanoTime() - start < timeout) {
						if (!c.getNext()) {
							return null;
						}

						consumer.accept(c.getKey(), c.getValue());
					}
					return c.getKey();
				}finally {
					t.commit();
				}
			});
		} while (lastKey != null);

		activeThreads.remove(Thread.currentThread());
	}

	public boolean update(ByteIterable key, ByteIterable value) {
		return environment.computeInTransaction(t -> store.put(t, key, value));
	}
	
	public boolean remove(ByteIterable key) {
		return environment.computeInTransaction(t -> store.delete(t, key));
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

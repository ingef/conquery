package com.bakdata.conquery.io.storage.xodus.stores;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.primitives.Ints;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.Transaction;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XodusStore implements com.bakdata.conquery.io.storage.Store<ByteIterable, ByteIterable> {
	private final Store store;
	@Getter
	private final Environment environment;
	private final long timeoutHalfMillis; // milliseconds
	private final Consumer<XodusStore> storeCloseHook;
	private final Consumer<XodusStore> storeRemoveHook;
	@Getter
	private final String name;

	public XodusStore(Environment env, String name, Consumer<XodusStore> storeCloseHook, Consumer<XodusStore> storeRemoveHook) {
		// Arbitrary duration that is strictly shorter than the timeout to not get interrupted by StuckTxMonitor
		this.timeoutHalfMillis = env.getEnvironmentConfig().getEnvMonitorTxnsTimeout() / 2;
		this.name = name;
		this.environment = env;
		this.storeCloseHook = storeCloseHook;
		this.storeRemoveHook = storeRemoveHook;
		this.store = env.computeInTransaction(
				t -> env.openStore(this.name, StoreConfig.WITHOUT_DUPLICATES_WITH_PREFIXING, t)
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
	 *
	 * @param consumer function called for-each key-value pair.
	 */
	@Override
	public SerializingStore.IterationStatistic forEach(StoreEntryConsumer<ByteIterable, ByteIterable> consumer) {
		AtomicReference<ByteIterable> lastKey = new AtomicReference<>();
		AtomicBoolean done = new AtomicBoolean(false);
		while (!done.get()) {
			environment.executeInReadonlyTransaction(t -> {
				try (Cursor c = store.openCursor(t)) {
					//try to load everything in the same transaction
					//but keep within half of the timeout time
					long start = System.currentTimeMillis();
					//search where we left of
					if (lastKey.get() != null) {
						c.getSearchKey(lastKey.get());
					}
					while (System.currentTimeMillis() - start < timeoutHalfMillis) {
						if (!c.getNext()) {
							done.set(true);
							return;
						}
						lastKey.set(c.getKey());
						ByteIterable value = c.getValue();
						consumer.accept(lastKey.get(), value, value.getLength());
					}
				}
			});
		}

		return null;
	}

	public Stream<ByteIterable> getAllKeys() {
		XodusKeyIterator spliterator = new XodusKeyIterator(environment, store, timeoutHalfMillis);
		return StreamSupport.stream(spliterator, false).onClose(spliterator::onClose);
	}

	public boolean update(ByteIterable key, ByteIterable value) {
		return environment.computeInTransaction(t -> store.put(t, key, value));
	}

	public boolean remove(ByteIterable key) {
		return environment.computeInTransaction(t -> store.delete(t, key));
	}

	@Override
	public boolean hasKey(ByteIterable byteIterable) {
		// This is hopefully never called.
		return environment.computeInReadonlyTransaction(tx -> {
			try (Cursor cursor = store.openCursor(tx)) {
				cursor.getSearchKey(byteIterable);
				return byteIterable.equals(cursor.getKey());
			}
		});
	}

	public int count() {
		return Ints.checkedCast(environment.computeInReadonlyTransaction(store::count));
	}

	@Override
	public Stream<ByteIterable> getAll() {
		return Stream.empty();
	}


	public void clear() {
		environment.executeInExclusiveTransaction(t -> {
			try (Cursor cursor = store.openCursor(t)) {
				while (cursor.getNext()) {
					cursor.deleteCurrent();
				}
			}
		});
	}

	@Override
	public boolean contains(ByteIterable key) {
		return get(key) != null;
	}

	@Override
	public void invalidateCache() {
		/* Do nothing, no caches here */
	}

	@Override
	public void loadKeys() {

	}

	public void removeStore() {
		log.debug("Deleting store {} from environment {}", store.getName(), environment.getLocation());
		environment.executeInTransaction(t -> environment.removeStore(store.getName(), t));
		storeRemoveHook.accept(this);
	}

	@Override
	public void loadData() {

	}

	public void close() {
		if (!environment.isOpen()) {
			log.trace("While closing store: Environment is already closed for {}", this);
			return;
		}
		storeCloseHook.accept(this);
	}

	@Override
	public String toString() {
		return "XodusStore[" + environment.getLocation() + ":" + store.getName() + "}";
	}

	private static class XodusKeyIterator extends Spliterators.AbstractSpliterator<ByteIterable> {

		private final long timeoutHalfMillis;
		private final Environment environment;
		private final Store store;
		private final AtomicReference<ByteIterable> lastKey = new AtomicReference<>();
		private Transaction transaction;
		private Cursor cursor;
		private long start;

		protected XodusKeyIterator(Environment environment, Store store, long timeoutHalfMillis) {
			super(Long.MAX_VALUE, Spliterator.ORDERED);
			this.timeoutHalfMillis = timeoutHalfMillis;
			this.environment = environment;
			this.store = store;

			refreshCursor();
		}

		private void refreshCursor() {
			if (transaction != null && !transaction.isFinished()) {
				transaction.abort();
			}

			start = System.currentTimeMillis();
			transaction = environment.beginReadonlyTransaction();
			cursor = store.openCursor(transaction);

			if (lastKey.get() != null) {
				cursor.getSearchKey(lastKey.get());
			}
		}

		@Override
		public boolean tryAdvance(Consumer<? super ByteIterable> action) {
			if (System.currentTimeMillis() - start >= timeoutHalfMillis) {
				// refresh transaction after half of the timeout
				refreshCursor();
			}

			if (cursor.getNext()) {
				ByteIterable key = cursor.getKey();
				lastKey.set(key);
				action.accept(key);
				return true;
			}
			else {
				cursor.close();
				onClose();
				return false;
			}
		}

		public void onClose() {
			if (!transaction.isFinished()) {
				transaction.abort();
			}
		}
	}
}

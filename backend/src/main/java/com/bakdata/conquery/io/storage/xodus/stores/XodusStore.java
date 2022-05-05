package com.bakdata.conquery.io.storage.xodus.stores;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.bakdata.conquery.io.storage.RawStore;
import com.google.common.primitives.Ints;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XodusStore implements RawStore {
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

	public boolean add(byte[] key, byte[] value) {
		return environment.computeInTransaction(t -> store.add(t, new ArrayByteIterable(key), new ArrayByteIterable(value)));
	}

	public byte[] get(byte[] key) {
		return environment.computeInReadonlyTransaction(t -> store.get(t, new ArrayByteIterable(key))).getBytesUnsafe();
	}

	/**
	 * Iterate over all key-value pairs in a consistent manner.
	 * The transaction is read only!
	 *
	 * @param consumer function called for-each key-value pair.
	 */
	public void forEach(BiConsumer<byte[], byte[]> consumer) {
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
					while(System.currentTimeMillis()-start < timeoutHalfMillis) {
						if(!c.getNext()) {
							done.set(true);
							return;
						}
						lastKey.set(c.getKey());
						consumer.accept(lastKey.get().getBytesUnsafe(), c.getValue().getBytesUnsafe());
					}
				}
			});
		}
	}

	public boolean update(byte[] key, byte[] value) {
		return environment.computeInTransaction(t -> store.put(t, new ArrayByteIterable(key), new ArrayByteIterable(value)));
	}

	public boolean remove(byte[] key) {
		return environment.computeInTransaction(t -> store.delete(t, new ArrayByteIterable(key)));
	}

	public int count() {
		return Ints.checkedCast(environment.computeInReadonlyTransaction(store::count));
	}


	public void clear() {
		environment.executeInExclusiveTransaction(t -> {
			Cursor cursor = store.openCursor(t);
			while(cursor.getNext()){
				cursor.deleteCurrent();
			}
		});
	}

	public void deleteStore() {
		log.debug("Deleting store {} from environment {}", store.getName(), environment.getLocation());
		environment.executeInTransaction(t -> environment.removeStore(store.getName(),t));
		storeRemoveHook.accept(this);
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
		return "XodusStore[" + environment.getLocation() + ":" +store.getName() +"}";
	}
}

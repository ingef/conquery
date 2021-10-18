package com.bakdata.conquery.models.identifiable;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import com.bakdata.conquery.models.identifiable.ids.IId;

public class IdMutex<T extends IId<?>> {
	private final ConcurrentHashMap<T, Locked> mutexMap = new ConcurrentHashMap<>();

	public Locked acquire(final T key) {
		Locked lock = mutexMap.get(key);
		if (lock == null) {
			synchronized (mutexMap) {
				lock = mutexMap.get(key);
				if (lock == null) {
					lock = new Locked();
					mutexMap.put(key, lock);
				}
			}
		}
		lock.acquireUninterruptibly();
		return lock;
	}

	public static final class Locked extends Semaphore implements Closeable {
		private static final long serialVersionUID = 1L;

		public Locked() {
			super(1);
		}

		@Override
		public void close() {
			this.release();
		}
	}
}

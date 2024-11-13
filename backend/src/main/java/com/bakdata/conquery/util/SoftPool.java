package com.bakdata.conquery.util;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import com.bakdata.conquery.models.config.ClusterConfig;
import com.google.common.util.concurrent.Uninterruptibles;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SoftPool<T> {

	private final ConcurrentLinkedDeque<SoftReference<T>> pool = new ConcurrentLinkedDeque<>();
	private final AtomicLong poolSize = new AtomicLong(0);
	private final Supplier<T> supplier;
	private final ClusterConfig config;
	private final Thread poolCleaner = new Thread(this::cleanPool, "SoftPool Cleaner");

	public SoftPool(ClusterConfig config, Supplier<T> supplier) {
		this.config = config;
		this.supplier = supplier;

		// Should not prevent the JVM shutdown -> daemon

		poolCleaner.setDaemon(true);
		poolCleaner.start();
	}

	/**
	 * Offer/return a reusable object to the pool.
	 *
	 * @param v the object to return to the pool.
	 */
	public void offer(T v) {
		pool.addLast(new SoftReference<>(v));
		log.trace("Pool size: {} (offer)", poolSize.incrementAndGet());
	}

	private void cleanPool() {
		while (true) {
			Uninterruptibles.sleepUninterruptibly(config.getSoftPoolCleanerPause().toSeconds(), TimeUnit.SECONDS);

			log.trace("Running pool cleaner");
			while (poolSize.get() > config.getSoftPoolBaselineSize()) {
				// Poll until we reached the baseline
				borrow();
			}
		}
	}

	/**
	 * Returns a reusable element from the pool if available or
	 * returns a new element from the provided supplier.
	 */
	public T borrow() {
		SoftReference<T> result;

		// First check the pool for available/returned elements
		while ((result = pool.poll()) != null) {
			log.trace("Pool size: {} (borrow)", poolSize.decrementAndGet());
			// The pool had an element, inspect if it is still valid
			final T elem = result.get();
			if (elem != null) {
				// Return valid element
				return elem;
			}
			// Referenced element was already garbage collected. Poll further
		}
		// Pool was empty -- request a new element
		return supplier.get();
	}
}

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
	private final Thread poolCleaner;
	private final long softPoolBaselineSize;
	private final long cleanerPauseSeconds;

	public SoftPool(ClusterConfig config, Supplier<T> supplier) {
		this.supplier = supplier;

		softPoolBaselineSize = config.getSoftPoolBaselineSize();
		cleanerPauseSeconds = config.getSoftPoolCleanerPause().toSeconds();

		if (softPoolBaselineSize <= 0 || cleanerPauseSeconds <= 0) {
			log.debug("Not creating a Cleaner.");
			poolCleaner = null;
			return;
		}

		poolCleaner = new Thread(this::cleanPool, "SoftPool Cleaner");
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

		final long currentPoolSize = poolSize.incrementAndGet();

		log.trace("Pool size: {} (offer)", currentPoolSize);
	}

	/**
	 * Returns a reusable element from the pool if available or
	 * returns a new element from the provided supplier.
	 */
	public T borrow() {
		SoftReference<T> result;

		// First check the pool for available/returned elements
		while ((result = pool.poll()) != null) {
			final long currentPoolSize = poolSize.decrementAndGet();

			log.trace("Pool size: {} (borrow)", currentPoolSize);

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

	/**
	 * Trims the pool in a custom interval so that soft references get purged earlier
	 */
	private void cleanPool() {
		while (true) {
			Uninterruptibles.sleepUninterruptibly(cleanerPauseSeconds, TimeUnit.SECONDS);

			log.trace("Running pool cleaner");
			while (poolSize.get() > softPoolBaselineSize) {
				// Poll until we reached the baseline
				borrow();
			}
		}
	}
}

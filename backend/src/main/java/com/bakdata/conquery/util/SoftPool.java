package com.bakdata.conquery.util;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SoftPool<T> {

	private final ConcurrentLinkedDeque<SoftReference<T>> pool = new ConcurrentLinkedDeque<>();
	private final Supplier<T> supplier;

	/**
	 * Returns a reusable element from the pool if available or
	 * returns a new element from the provided supplier.
	 */
	public T borrow() {
		SoftReference<T> result;
		// First check the pool for available/returned elements
		while ((result = pool.poll()) != null) {
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
	 * Offer/return a reusable object to the pool.
	 * @param v the object to return to the pool.
	 */
	public void offer(T v) {
		pool.addLast(new SoftReference<>(v));
	}
}

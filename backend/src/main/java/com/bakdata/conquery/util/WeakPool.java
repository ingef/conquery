package com.bakdata.conquery.util;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WeakPool<T> {

	private final ConcurrentLinkedDeque<WeakReference<T>> pool = new ConcurrentLinkedDeque<>();
	private final Supplier<T> supplier;

	public T borrow() {
		WeakReference<T> result;
		while ((result = pool.poll()) != null) {

			final T elem = result.get();
			if (elem != null) {
				return elem;
			}
		}
		return supplier.get();
	}

	public void returnValue(T v) {
		pool.addLast(new WeakReference<>(v));
	}
}

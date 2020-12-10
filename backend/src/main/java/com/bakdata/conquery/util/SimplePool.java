package com.bakdata.conquery.util;

import java.util.concurrent.ConcurrentLinkedDeque;

import com.google.common.base.Supplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SimplePool<T> {
	
	private final ConcurrentLinkedDeque<T> pool = new ConcurrentLinkedDeque<>();
	private final Supplier<T> supplier;
	
	public T borrow() {
		T result = pool.poll();
		if(result == null) {
			return supplier.get();
		}
		return result;
	}
	
	public void returnValue(T v) {
		pool.addLast(v);
	}
}

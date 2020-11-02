package com.bakdata.conquery.io;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.AbstractIterator;
import lombok.extern.slf4j.Slf4j;

/**
 * Iterator asynchronously prefetching values of supplied iterator. Can be used when supplied iterator comes from IO.
 */
@Slf4j
public class PrefetchingIterator<T> extends AbstractIterator<T> implements Closeable {

	private final BlockingQueue<Object> queue;
	private final Object marker = new Object();

	private AtomicBoolean isRunning = new AtomicBoolean(true);

	public PrefetchingIterator(Iterator<T> iterator, int capacity) {
		queue = new ArrayBlockingQueue<>(capacity);
		Thread thread = new Thread("Async Iterator Thread") {
			@Override
			public void run() {
				try {
					while (iterator.hasNext() && isRunning.get()) {
						queue.put(iterator.next());
					}
				} catch (Exception e) {
					log.error("Exception in Async Iterator", e);
				} finally {
					try {
						queue.put(marker);
					} catch (InterruptedException e) {
						log.error("Interrupted in Async Iterator Thread", e);
					}
				}
			}
		};
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	protected T computeNext() {
		try {
			Object next = queue.take();
			if(next == marker) {
				return endOfData();
			}
			return (T) next;
		}
		catch(InterruptedException e) {
			throw new RuntimeException("Interrupted in CSV Parsing Thread", e);
		}
	}

	@Override
	public void close() throws IOException {
		isRunning.set(false);
	}
}

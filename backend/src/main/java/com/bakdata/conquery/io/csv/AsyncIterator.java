package com.bakdata.conquery.io.csv;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.google.common.collect.AbstractIterator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AsyncIterator<T> extends AbstractIterator<T> {

	private final Iterator<T> iterator;
	private final BlockingQueue<Object> queue = new ArrayBlockingQueue<>(100);
	private final Object marker = new Object();
	private final Thread thread;
	
	public AsyncIterator(Iterator<T> iterator) {
		this.iterator = iterator;
		thread = new Thread("CSV Parsing Thread") {
			@Override
			public void run() {
				try {
					while(iterator.hasNext()) {
						queue.put(iterator.next());
					}
				}
				catch(Exception e) {
					log.error("Exception in CSV Parsing Thread", e);
				}
				finally {
					try {
						queue.put(marker);
					}
					catch (InterruptedException e) {
						log.error("Interrupted CSV Parsing Thread", e);
					}
				}
			}
		};
		thread.start();
	}

	@Override
	protected T computeNext() {
		try {
			Object next = queue.take();
			if(next == marker) {
				return endOfData();
			}
			else {
				return (T) next;
			}
		}
		catch(InterruptedException e) {
			throw new RuntimeException("Interrupted in CSV Parsing Thread", e);
		}
	}
}

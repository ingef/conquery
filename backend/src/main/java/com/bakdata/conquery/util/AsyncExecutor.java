package com.bakdata.conquery.util;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class AsyncExecutor implements Closeable {

	private final ExecutorService service;

	public AsyncExecutor(String name) {
		this(name, false);
	}
	
	public AsyncExecutor(String name, boolean restricted) {
		if(restricted) {
			service = new ThreadPoolExecutor(0, 5,
					10L, TimeUnit.SECONDS,
					new LinkedBlockingQueue<>(),
					new ThreadFactoryBuilder()
						.setNameFormat(name)
						.build(),
					new ThreadPoolExecutor.CallerRunsPolicy()
			);
		}
		else {
			service = Executors.newCachedThreadPool(
					new ThreadFactoryBuilder()
					.setNameFormat(name)
					.build()
			);
		}
	}
	
	public void execute(Runnable r) {
		service.execute(r);
	}

	@Override
	public void close() {
		service.shutdown();
		try {
			service.awaitTermination(10, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}
}

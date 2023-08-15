package com.bakdata.conquery.util;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import lombok.Data;

@Data
public class CallerBlocksRejectionHandler implements RejectedExecutionHandler {

	private final long timeoutMillis;
	private final LongAdder waitedMillis = new LongAdder();

	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		if (executor.isShutdown()) {
			return;
		}

		try {
			long before = System.currentTimeMillis();
			final boolean success = executor.getQueue().offer(r, getTimeoutMillis(), TimeUnit.MILLISECONDS);
			long after = System.currentTimeMillis();

			waitedMillis.add(after - before);

			if (!success) {
				throw new RejectedExecutionException("Could not submit within specified timeout.");
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RejectedExecutionException("Thread was interrupted.");
		}
	}
}

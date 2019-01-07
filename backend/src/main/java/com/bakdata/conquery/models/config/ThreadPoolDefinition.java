package com.bakdata.conquery.models.config;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import javax.validation.constraints.Min;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.dropwizard.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class ThreadPoolDefinition {
	@Min(0)
	private int minThreads = 0;
	@Min(1)
	private int maxThreads = Runtime.getRuntime().availableProcessors();
	private boolean allowCoreThreadTimeOut = false;
	private Duration keepAliveTime = Duration.seconds(60);
	private Duration shutdownTime = Duration.hours(1);
	
	public ListeningExecutorService createService(String nameFormat) {
		final ThreadPoolExecutor executor = new ThreadPoolExecutor(
				minThreads,
				maxThreads,
				keepAliveTime.getQuantity(),
				keepAliveTime.getUnit(),
				new LinkedBlockingQueue<>(),
				new ThreadFactoryBuilder().setNameFormat(nameFormat).build()
		);
		executor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
		return MoreExecutors.listeningDecorator(executor);
	}
}

package com.bakdata.conquery.models.config;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import javax.validation.constraints.Min;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ThreadPoolDefinition {
	@Min(0)
	private int minThreads = Runtime.getRuntime().availableProcessors();
	@Min(1)
	private int maxThreads = Runtime.getRuntime().availableProcessors();
	private boolean allowCoreThreadTimeOut = false;
	private Duration keepAliveTime = Duration.seconds(60);
	private Duration shutdownTime = Duration.hours(1);

	public ThreadPoolExecutor createService(String nameFormat) {
		final ThreadPoolExecutor executor = new ThreadPoolExecutor(
				minThreads,
				maxThreads,
				keepAliveTime.getQuantity(),
				keepAliveTime.getUnit(),
				new LinkedBlockingQueue<>(),
				new ThreadFactoryBuilder().setNameFormat(nameFormat).build()
		);
		executor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
		return executor;
	}
}

package com.bakdata.conquery.metrics;

import java.util.concurrent.LinkedBlockingDeque;

import com.bakdata.conquery.io.storage.ConqueryStorage;
import com.bakdata.conquery.models.jobs.Job;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JobMetrics {

	private static final String JOBS = "jobs";
	private static final String EXECUTION_TIME = "time";
	private static final String QUEUE_SIZE = "queue";

	public static Timer.Context getStoreLoadingTimer() {
		return SharedMetricRegistries.getDefault().timer(MetricRegistry.name(JOBS, EXECUTION_TIME, ConqueryStorage.class.getSimpleName())).time();
	}

	public static void createJobQueueGauge(String name, LinkedBlockingDeque<Job> jobs) {
		SharedMetricRegistries.getDefault().gauge(MetricRegistry.name(JOBS, QUEUE_SIZE, name), () -> jobs::size);
	}

	public static void removeJobQueueSizeGauge(String name) {
		SharedMetricRegistries.getDefault().remove(MetricRegistry.name(JOBS, QUEUE_SIZE, name));
	}

	public static Timer.Context getJobExecutorTimer(Job job) {
		return SharedMetricRegistries.getDefault().timer(MetricRegistry.name(JOBS, EXECUTION_TIME, job.getClass().getSimpleName())).time();
	}
}

package com.bakdata.conquery.metrics;

import java.util.concurrent.LinkedBlockingDeque;

import com.bakdata.conquery.io.xodus.ConqueryStorage;
import com.bakdata.conquery.models.jobs.Job;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

public class MetricsUtil {

	public static Timer.Context getStoreLoadingTimer() {
		return SharedMetricRegistries.getDefault().timer(MetricRegistry.name( "jobs", "time", ConqueryStorage.class.getSimpleName())).time();
	}

	public static void createJobQueueGauge(String name, LinkedBlockingDeque<Job> jobs) {
		SharedMetricRegistries.getDefault().register(MetricRegistry.name("jobs", "queue", name), (Gauge<Integer>) jobs::size);
	}

	public static void removeJobQueueSizeGauge(String name) {
		SharedMetricRegistries.getDefault().remove(MetricRegistry.name("jobs",  "queue", name));
	}

	public static Timer.Context getJobExecutorTimer(Job job) {
		return SharedMetricRegistries.getDefault().timer(MetricRegistry.name("jobs", "time", job.getClass().getSimpleName())).time();
	}
}

package com.bakdata.conquery.models.config;

import io.dropwizard.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class QueryConfig {

	private int roundRobinQueueCapacity = 10;
	private int nThreads = Runtime.getRuntime().availableProcessors();

	private Duration oldQueriesTime = Duration.days(30);
}

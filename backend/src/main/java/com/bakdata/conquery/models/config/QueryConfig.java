package com.bakdata.conquery.models.config;

import io.dropwizard.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class QueryConfig {

	private ThreadPoolDefinition executionPool = new ThreadPoolDefinition();

	private Duration oldQueriesTime = Duration.days(30);
}

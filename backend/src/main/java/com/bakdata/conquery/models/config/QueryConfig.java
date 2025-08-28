package com.bakdata.conquery.models.config;

import com.bakdata.conquery.util.validation.ValidCaffeineSpec;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class QueryConfig {

	private ThreadPoolDefinition executionPool = new ThreadPoolDefinition();

	private Duration oldQueriesTime = Duration.days(30);

	/**
	 * Limits how many subQuery-Plans should be cached between executions:
	 * This number limits how many sub-plans are cached per core so that outliers do not cause massive memory overhead.
	 *
	 * TODO Implement global limit of active secondaryId sub plans
	 */
	private int secondaryIdSubPlanRetention = 15;


	/**
	 * See {@link com.bakdata.conquery.models.query.ExecutionManager#executionInfosL1} for an explanation
	 */
	@ValidCaffeineSpec
	private String L1CacheSpec = "expireAfterAccess=10m";

	/**
	 * See {@link com.bakdata.conquery.models.query.ExecutionManager#executionInfosL1} for an explanation
	 */
	@ValidCaffeineSpec(softValue=true)
	private String L2CacheSpec = "softValues";

}

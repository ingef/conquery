package com.bakdata.conquery.integration.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.models.auth.DevAuthConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryToCSVRenderer;
import com.bakdata.conquery.models.query.concept.ResultInfo;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.FailedEntityResult;
import com.bakdata.conquery.models.query.results.MultilineContainedEntityResult;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.powerlibraries.io.In;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractQueryEngineTest extends ConqueryTestSpec {

	@JsonIgnore
	protected abstract IQuery getQuery();

	protected abstract ResourceFile getExpectedCsv();

	@JsonIgnore
	private static final PrintSettings PRINT_SETTINGS = PrintSettings
		.builder()
		.prettyPrint(false)
		.nameExtractor(
			sd -> sd.getCqConcept().getIds().get(0).toStringWithoutDataset() + "_" + sd.getSelect().getId().toStringWithoutDataset())
		.build();

	@Override
	public void executeTest(StandaloneSupport standaloneSupport) throws IOException, JSONException {
		IQuery query = getQuery();

		ManagedQuery managed = standaloneSupport.getNamespace().getQueryManager().createQuery(query, DevAuthConfig.USER);

		managed.awaitDone(10, TimeUnit.SECONDS);
		while(managed.getState()!=ExecutionState.DONE && managed.getState()!=ExecutionState.FAILED) {
			log.warn("waiting for more than 10 seconds on "+getLabel());
			managed.awaitDone(1, TimeUnit.DAYS);
		}

		if (managed.getState() == ExecutionState.FAILED) {
			managed
				.getResults()
				.stream()
				.filter(EntityResult::isFailed)
				.map(FailedEntityResult.class::cast)
				.forEach(r->log.error("Failure in query {}: {}", managed.getId(), r.getExceptionStackTrace()));
			fail("Query failed (see above)");
		}
		
		//check result info size
		List<ResultInfo> resultInfos = managed.getResultInfos(PRINT_SETTINGS);
		assertThat(
			managed
				.fetchContainedEntityResult()
				.flatMap(ContainedEntityResult::streamValues)
		)
		.allSatisfy(v->assertThat(v).hasSameSizeAs(resultInfos));

		List<String> actual = new QueryToCSVRenderer()
			.toCSV(PRINT_SETTINGS, managed)
			.collect(Collectors.toList());

		ResourceFile expectedCsv = getExpectedCsv();

		List<String> expected = In.stream(expectedCsv.stream()).readLines();

		assertThat(actual).as("Results for %s are not as expected.", this).containsExactlyInAnyOrderElementsOf(expected);
		// check that getLastResultCount returns the correct size
		if (managed.fetchContainedEntityResult().noneMatch(MultilineContainedEntityResult.class::isInstance)) {
			assertThat(managed.getLastResultCount()).as("Result count for %s is not as expected.", this).isEqualTo(expected.size() - 1);
		}

		log.info("INTEGRATION TEST SUCCESSFUL {} {} on {} rows", getClass().getSimpleName(), this, expected.size());
	}
}

package com.bakdata.conquery.integration.json;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.validation.UnexpectedTypeException;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.bakdata.conquery.resources.api.ResultCsvResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.sql.conquery.SqlManagedQuery;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractQueryEngineTest extends ConqueryTestSpec {

	@Override
	public void executeTest(StandaloneSupport standaloneSupport) throws IOException {
		Query query = getQuery();

		assertThat(standaloneSupport.getValidator().validate(query))
				.describedAs("Query Validation Errors")
				.isEmpty();


		log.info("{} QUERY INIT", getLabel());

		final User testUser = standaloneSupport.getTestUser();

		final ManagedExecutionId executionId = IntegrationUtils.assertQueryResult(standaloneSupport, query, -1, ExecutionState.DONE, testUser, 201);

		final ManagedExecution execution = standaloneSupport.getMetaStorage().getExecution(executionId);
		SingleTableResult executionResult = (SingleTableResult) execution;

		//check result info size
		List<ResultInfo> resultInfos = executionResult.getResultInfos();

		assertThat(executionResult.streamResults().flatMap(EntityResult::streamValues))
				.as("Should have same size as result infos")
				.allSatisfy(v -> assertThat(v).hasSameSizeAs(resultInfos));

		// Get the actual response and compare with expected result.
		final Response csvResponse =
				standaloneSupport.getClient()
								 .target(HierarchyHelper.hierarchicalPath(standaloneSupport.defaultApiURIBuilder(), ResultCsvResource.class, "getAsCsv")
														.buildFromMap(
																Map.of(
																		DATASET, standaloneSupport.getDataset().getName(),
																		QUERY, execution.getId().toString()
																)
														))
								 .queryParam("pretty", false)
								 .request(AdditionalMediaTypes.CSV)
								 .acceptLanguage(Locale.ENGLISH)
								 .get();

		List<String> actual = In.stream(((InputStream) csvResponse.getEntity())).readLines();

		ResourceFile expectedCsv = getExpectedCsv();

		List<String> expected = In.stream(expectedCsv.stream()).readLines();

		assertThat(actual).as("Results for %s are not as expected.", this)
						  .containsExactlyInAnyOrderElementsOf(expected);

		// check that getLastResultCount returns the correct size
		if (executionResult.streamResults().noneMatch(MultilineEntityResult.class::isInstance)) {
			long lastResultCount;
			// TODO find common abstraction for Sql/ManagedQuery
			if (executionResult instanceof ManagedQuery managedQuery) {
				lastResultCount = managedQuery.getLastResultCount();
			}
			else if (executionResult instanceof SqlManagedQuery sqlManagedQuery) {
				lastResultCount = sqlManagedQuery.getLastResultCount();
			}
			else {
				throw new UnexpectedTypeException("Did not expect a ManagedExecution of type %s.".formatted(execution.getClass()));
			}
			assertThat(lastResultCount).as("Result count for %s is not as expected.", this).isEqualTo(expected.size() - 1);
		}

		log.info("INTEGRATION TEST SUCCESSFUL {} {} on {} rows", getClass().getSimpleName(), this, expected.size());
	}

	@JsonIgnore
	protected abstract Query getQuery();

	protected abstract ResourceFile getExpectedCsv();
}

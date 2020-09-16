package com.bakdata.conquery.integration.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.bakdata.conquery.integration.common.ResourceFile;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.QueryToCSVRenderer;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.MultilineContainedEntityResult;
import com.bakdata.conquery.models.worker.Namespaces;
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
	private static final PrintSettings PRINT_SETTINGS = new PrintSettings(false,Locale.ENGLISH, columnInfo -> columnInfo.getSelect().getId().toStringWithoutDataset());

	@Override
	public void executeTest(StandaloneSupport standaloneSupport) throws IOException, JSONException {
		Namespaces namespaces = standaloneSupport.getNamespace().getNamespaces();
		MasterMetaStorage storage = standaloneSupport.getNamespace().getStorage().getMetaStorage();
		UserId userId = standaloneSupport.getTestUser().getId();
		DatasetId dataset = standaloneSupport.getNamespace().getDataset().getId();
		
		IQuery query = getQuery();

		log.info("{} QUERY INIT", getLabel());
		query.resolve(new QueryResolveContext(dataset, namespaces));
		
		ManagedQuery managed = (ManagedQuery) ExecutionManager.runQuery(namespaces, query, userId, dataset);

		managed.awaitDone(10, TimeUnit.SECONDS);
		while(managed.getState()!=ExecutionState.DONE && managed.getState()!=ExecutionState.FAILED) {
			log.warn("waiting for more than 10 seconds on "+getLabel());
			managed.awaitDone(1, TimeUnit.DAYS);
		}

		if (managed.getState() == ExecutionState.FAILED) {
			log.error("Failure in Query[{}]. The error was: {}" + managed.getId(),managed.getError());
			fail("Query failed (see above)");
		}
		
		//check result info size
		ResultInfoCollector resultInfos = managed.collectResultInfos();
		assertThat(
			managed
				.fetchContainedEntityResult()
				.flatMap(ContainedEntityResult::streamValues)
		)
		.as("Should have same size as result infos")
		.allSatisfy(v->
			assertThat(v).hasSameSizeAs(resultInfos.getInfos())
		);

		List<String> actual = QueryToCSVRenderer
			.toCSV(
				PRINT_SETTINGS,
				managed,
				standaloneSupport.getConfig().getIdMapping()
					.initToExternal(standaloneSupport.getTestUser(), managed))
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

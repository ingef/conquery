package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.SecondaryIdQuery;
import com.bakdata.conquery.models.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ReusedQueryTest implements ProgrammaticIntegrationTest {

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {

		final StandaloneSupport conquery = testConquery.getSupport(name);


		final String testJson = In.resource("/tests/query/SECONDARY_ID/SECONDARY_IDS.test.json").withUTF8().readAll();

		final Dataset dataset = conquery.getDataset();


		final QueryTest test = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson);

		// Manually import data, so we can do our own work.
		{
			ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

			importSecondaryIds(conquery, test.getContent().getSecondaryIds());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTables(conquery, test.getContent());
			conquery.waitUntilWorkDone();

			LoadingUtil.importConcepts(conquery, test.getRawConcepts());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTableContents(conquery, test.getContent().getTables(), conquery.getDataset());
			conquery.waitUntilWorkDone();
		}

		final SecondaryIdQuery query = (SecondaryIdQuery) IntegrationUtils.parseQuery(conquery, test.getRawQuery());

		final ManagedExecutionId id = IntegrationUtils.assertQueryResult(conquery, query, 5L, ExecutionState.DONE);

		assertThat(id).isNotNull();

		// Normal reuse
		{
			final ConceptQuery reused = new ConceptQuery(new CQReusedQuery(id));

			IntegrationUtils.assertQueryResult(conquery, reused, 2L, ExecutionState.DONE);
		}

		// Reuse in SecondaryId
		{
			final SecondaryIdQuery reused = new SecondaryIdQuery();
			reused.setRoot(new CQReusedQuery(id));

			reused.setSecondaryId(conquery.getNamespace().getStorage().getSecondaryIds().get(0));

			IntegrationUtils.assertQueryResult(conquery, reused, 5L, ExecutionState.DONE);
		}

		// Reuse Multiple times with different query types
		{
			final SecondaryIdQuery reused1 = new SecondaryIdQuery();
			reused1.setRoot(new CQReusedQuery(id));

			reused1.setSecondaryId(query.getSecondaryId());

			final ManagedExecutionId reused1Id = IntegrationUtils.assertQueryResult(conquery, reused1, 5L, ExecutionState.DONE);

			final SecondaryIdQuery reused2 = new SecondaryIdQuery();
			reused2.setRoot(new CQReusedQuery(reused1Id));

			reused2.setSecondaryId(query.getSecondaryId());

			final ManagedExecutionId reused2Id = IntegrationUtils.assertQueryResult(conquery, reused2, 5L, ExecutionState.DONE);

			assertThat(reused2Id)
					.as("Query should be reused.")
					.isEqualTo(reused1Id);

			final ConceptQuery reused3 = new ConceptQuery(new CQReusedQuery(reused2Id));

			IntegrationUtils.assertQueryResult(conquery, reused3, 2L, ExecutionState.DONE);
		}

	}
}

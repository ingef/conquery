package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.statistics.DateColumnStatsCollector;
import com.bakdata.conquery.models.query.statistics.ResultStatistics;
import com.bakdata.conquery.models.query.statistics.StringColumnStatsCollector;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.api.QueryResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueryStatisticsTest implements ProgrammaticIntegrationTest {


	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {

		final StandaloneSupport conquery = testConquery.getSupport(name);

		final String testJson = In.resource("/tests/query/QUERY_STATISTICS_TESTS/SIMPLE_TREECONCEPT_Query.json").withUTF8().readAll();

		final Dataset dataset = conquery.getDataset();

		final QueryTest test = JsonIntegrationTest.readJson(dataset, testJson);
		ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

		test.importRequiredData(conquery);

		conquery.waitUntilWorkDone();

		final ManagedExecutionId executionId =
				IntegrationUtils.assertQueryResult(conquery, test.getQuery(), 6, ExecutionState.DONE, conquery.getTestUser(), 201);

		final UriBuilder uriBuilder = HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), QueryResource.class, "getDescription");

		final ResultStatistics results = conquery.getClient().target(
														 uriBuilder.buildFromMap(Map.of(ResourceConstants.QUERY, executionId.toString())))
												 .request()
												 .acceptLanguage(Locale.ENGLISH)
												 .get(ResultStatistics.class);


		// We are using TreeMaps for Maps that have a defined order.
		final ResultStatistics expected = new ResultStatistics(
				6,
				6,
				List.of(
						new DateColumnStatsCollector.ColumnDescription(
								"dates",
								"dates",
								"",
								6,
								0,
								new TreeMap<>(Map.of(
										"2021-1", 5,
										"2021-4", 1
								)),
								new TreeMap<>(Map.of(
										"2021-01", 5,
										"2021-10", 1
								)),
								Range.of(
										LocalDate.of(2021, 1, 1), LocalDate.of(2021, 10, 1)
								)
						),
						new DateColumnStatsCollector.ColumnDescription(
								"concept date",
								"concept date",
								"This is a Description!",

								6,
								0,
								new TreeMap<>(Map.of(
										"2021-1", 5,
										"2021-4", 1
								)),
								new TreeMap<>(Map.of(
										"2021-01", 5,
										"2021-10", 1
								)),
								Range.of(
										LocalDate.of(2021, 1, 1), LocalDate.of(2021, 10, 1)
								)
						),
						new StringColumnStatsCollector.ColumnDescription(
								"concept string",
								"concept string",
								null,
								List.of(),
								Map.of()
						),
						new StringColumnStatsCollector.ColumnDescription(
								"concept mapped",
								"concept mapped",
								null,
								List.of(),
								Map.of()
						),
						new StringColumnStatsCollector.ColumnDescription(
								"concept int",
								"concept int",
								null,
								List.of(),
								Map.of()
						),
						new StringColumnStatsCollector.ColumnDescription(
								"concept real",
								"concept real",
								null,
								List.of(),
								Map.of()
						),
						new StringColumnStatsCollector.ColumnDescription(
								"concept decimal",
								"concept decimal",
								null,
								List.of(),
								Map.of()
						),
						new StringColumnStatsCollector.ColumnDescription(
								"concept money",
								"concept money",
								null,
								List.of(),
								Map.of()
						)
				),
				Range.of(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 10, 1))
		);

		assertThat(results)
				.usingRecursiveComparison()
				.isEqualTo(expected);

	}

}

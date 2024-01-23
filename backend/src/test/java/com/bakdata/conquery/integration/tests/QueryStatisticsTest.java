package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.core.UriBuilder;

import c10n.C10N;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.statistics.DateColumnStatsCollector;
import com.bakdata.conquery.models.query.statistics.HistogramColumnDescription;
import com.bakdata.conquery.models.query.statistics.ResultStatistics;
import com.bakdata.conquery.models.query.statistics.StatisticsLabels;
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

		final ResultStatistics results = conquery.getClient().target(uriBuilder.buildFromMap(Map.of(ResourceConstants.QUERY, executionId.toString())))
												 .request()
												 .acceptLanguage(Locale.ENGLISH)
												 .get(ResultStatistics.class);

		final StatisticsLabels labels = C10N.get(StatisticsLabels.class, Locale.ENGLISH);

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
						new HistogramColumnDescription(
								"concept string",
								"concept string",
								null,
								List.of(
										new HistogramColumnDescription.Entry("c", 2),
										new HistogramColumnDescription.Entry("a", 1),
										new HistogramColumnDescription.Entry("b", 1),
										new HistogramColumnDescription.Entry("d", 1)
								),
								Map.of()
						),
						new HistogramColumnDescription(
								"concept mapped",
								"concept mapped",
								null,
								List.of(
										new HistogramColumnDescription.Entry("CEH", 2),
										new HistogramColumnDescription.Entry("AH", 1),
										new HistogramColumnDescription.Entry("BEH", 1),
										new HistogramColumnDescription.Entry("d", 1)
								),
								Map.of()
						),
						new HistogramColumnDescription(
								"concept int",
								"concept int",
								null,
								List.of(
										new HistogramColumnDescription.Entry("1", 1),
										new HistogramColumnDescription.Entry("2", 1),
										new HistogramColumnDescription.Entry("3", 2),
										new HistogramColumnDescription.Entry("4", 1)
								),
								Map.of(labels.p25(), "1.5", labels.p75(), "3.5", labels.max(), "4", labels.mean(), "2.6", labels.median(), "3", labels.min(), "1", labels.missing(), "1", labels.std(), "1.14", labels.sum(), "13", labels.count(), "5")
						),
						new HistogramColumnDescription(
								"concept real",
								"concept real",
								null,
								List.of(
										new HistogramColumnDescription.Entry("1", 1),
										new HistogramColumnDescription.Entry("2", 1),
										new HistogramColumnDescription.Entry("3", 2),
										new HistogramColumnDescription.Entry("4", 1)
								),
								Map.of(labels.p25(), "1.5", labels.p75(), "3.5", labels.max(), "4", labels.mean(), "2.6", labels.median(), "3", labels.min(), "1", labels.missing(), "1", labels.std(), "1.14", labels.sum(), "13", labels.count(), "5")
						),
						new HistogramColumnDescription(
								"concept decimal",
								"concept decimal",
								null,
								List.of(
										new HistogramColumnDescription.Entry("1", 1),
										new HistogramColumnDescription.Entry("2", 1),
										new HistogramColumnDescription.Entry("3", 2),
										new HistogramColumnDescription.Entry("4", 1)
								),
								Map.of(labels.p25(), "1.5", labels.p75(), "3.5", labels.max(), "4", labels.mean(), "2.6", labels.median(), "3", labels.min(), "1", labels.missing(), "1", labels.std(), "1.14", labels.sum(), "13", labels.count(), "5")
						),
						new HistogramColumnDescription(
								"concept money",
								"concept money",
								null,
								List.of(
										new HistogramColumnDescription.Entry("€10.00", 1),
										new HistogramColumnDescription.Entry("€20.00", 1),
										new HistogramColumnDescription.Entry("€30.00", 2),
										new HistogramColumnDescription.Entry("€40.00", 1)
								),
								Map.of(labels.p25(), "€15.00", labels.p75(), "€35.00", labels.max(), "€40.00", labels.mean(), "€26.00", labels.median(), "€30.00", labels.min(), "€10.00", labels.missing(), "1", labels.std(), "11.4", labels.sum(), "€130.00", labels.count(), "5")
						),
						new HistogramColumnDescription(
								"concept boolean",
								"concept boolean",
								null,
								List.of(
										new HistogramColumnDescription.Entry("Yes", 4),
										new HistogramColumnDescription.Entry("No", 1)
								),
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

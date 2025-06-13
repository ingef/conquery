package com.bakdata.conquery.util.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.LabelMap;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.search.SolrConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.resources.api.ConceptsProcessor.AutoCompleteResult;
import com.bakdata.conquery.util.extensions.MockServerExtension;
import com.bakdata.conquery.util.extensions.SolrServerExtension;
import com.bakdata.conquery.util.progressreporter.ProgressReporterImpl;
import com.bakdata.conquery.util.search.solr.SolrBundle;
import com.bakdata.conquery.util.search.solr.SolrProcessor;
import com.github.powerlibraries.io.In;
import com.google.common.collect.ImmutableBiMap;
import com.univocity.parsers.csv.CsvParserSettings;
import io.dropwizard.core.setup.Environment;
import lombok.SneakyThrows;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.testcontainers.junit.jupiter.Testcontainers;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
public class SolrTest {


	public static final String MAPPING_PATH = "/shared/mapping.csv";
	public static final DatasetId DATASET_ID = new DatasetId("core1");
	public static final Column SEARCHABLE = createSearchable();
	public static final Environment ENVIRONMENT = new Environment(SolrTest.class.getSimpleName());
	public static final ConqueryConfig CONQUERY_CONFIG = new ConqueryConfig();
	@RegisterExtension
	private static final MockServerExtension REF_SERVER = new MockServerExtension(ClientAndServer.startClientAndServer(), SolrTest::initRefServer);
	@RegisterExtension
	private static final SolrServerExtension SOLR_SERVER = new SolrServerExtension(DATASET_ID.toString());
	private static final IndexService INDEX_SERVICE = new IndexService(new CsvParserSettings(){{setDelimiterDetectionEnabled(true);setLineSeparatorDetectionEnabled(true);}}, "emptyDefaultLabel");
	public static final SelectFilter<?> FILTER = createFilter();
	public static SolrConfig solrConfig;
	public static SolrProcessor searchProcessor;

	@SneakyThrows(IOException.class)
	public static void initRefServer(ClientAndServer mockServer) {

		try (InputStream inputStream = In.resource(MAPPING_PATH).asStream()) {
			mockServer.when(request().withPath("/mapping.csv"))
					  .respond(HttpResponse.response().withContentType(new MediaType("text", "csv")).withBody(inputStream.readAllBytes()));
		}

	}

	@BeforeAll
	public static void beforeAll() throws Exception {

		// Setup ref-server
		CONQUERY_CONFIG.getIndex().setBaseUrl(new URI(String.format("http://localhost:%d/", REF_SERVER.getPort())));

		SolrBundle solrBundle = new SolrBundle();

		String baseSolrUrl = SOLR_SERVER.getSolrBaseUrl();
		solrConfig = new SolrConfig(baseSolrUrl, "solr", "SolrRocks");
		CONQUERY_CONFIG.setSearch(solrConfig);
		solrBundle.run(CONQUERY_CONFIG, new Environment(SolrTest.class.getSimpleName()));
		searchProcessor = solrConfig.createSearchProcessor(ENVIRONMENT, DATASET_ID);

		// Cleanup core
		searchProcessor.clearSearch();
	}

	private static @NotNull SelectFilter<Object> createFilter() {
		return new SelectFilter<>() {

			@Override
			public FilterNode<?> createFilterNode(Object o) {
				throw new UnsupportedOperationException();
			}

			@Override
			public String getFilterType() {
				throw new UnsupportedOperationException();
			}

			@SneakyThrows(URISyntaxException.class)
			@Override
			public List<Searchable> getSearchReferences() {
				LabelMap labelMap = new LabelMap(getId(), ImmutableBiMap.of(
						"a", "Map A",
						"map b", "Map B",
						"map c", "Map C"
				), 0, false);

				final FilterTemplate index = new FilterTemplate(
						"test1",
						new URI("/mapping.csv"),
						"internal",
						"{{external}}",
						""
				);
				index.setIndexService(INDEX_SERVICE);
				index.setDataset(DATASET_ID);
				index.setConfig(CONQUERY_CONFIG);

				return new ArrayList<>(List.of(labelMap, index, SolrTest.SEARCHABLE));
			}

			@Override
			public FilterId getId() {
				return new FilterId(new ConnectorId(new ConceptId(DATASET_ID, "concept"), "connector"), "filter");
			}
		};
	}

	@Test
	@Order(0)
	public void addData() throws InterruptedException, SolrServerException, IOException {
		// Index values from concept/reference
		Set<Searchable> managerSearchables = FILTER.getSearchReferences().stream().filter(ref -> !(ref instanceof Column)).collect(Collectors.toSet());
		searchProcessor.indexManagerResidingSearches(managerSearchables, new AtomicBoolean(false), new ProgressReporterImpl());

		// Index values from column
		Column column = createSearchable();
		ArrayList<String> strings = new ArrayList<>(List.of(
				"a", // should be shadowed by LabelMap
				"b", // should be shadowed by external csv map
				"column c",
				"column ab",
				"column ba",
				"" // Empty string handling
		));

		// Null-Handling (adds null explicitly because List.of forbids it)
		strings.add(null);

		searchProcessor.registerValues(column, strings);
		searchProcessor.finalizeSearch(column);
		searchProcessor.explicitCommit();
	}

	private static @NotNull Column createSearchable() {
		Column column = new Column();
		column.setName("column1");
		Table table = new Table();
		table.setName("table");
		table.setDataset(DATASET_ID);
		column.setTable(table);
		return column;
	}

	@Test
	@Order(1)
	public void findExactColumn() {

		List<FrontendValue> actual = searchProcessor.findExact(FILTER, "column c");

		assertThat(actual).containsExactly(new FrontendValue("column c", "column c"));
	}

	@Test
	@Order(1)
	public void findExactMap() {
		List<FrontendValue> actualLabel = searchProcessor.findExact(FILTER, "Map A");

		assertThat(actualLabel).containsExactly(new FrontendValue("a", "Map A"));


		List<FrontendValue> actualValue = searchProcessor.findExact(FILTER, "map a");

		assertThat(actualValue).containsExactly(new FrontendValue("a", "Map A"));
	}

	@Test
	@Order(2)
	public void findEmptyTermFirstPage() {
		AutoCompleteResult actual = searchProcessor.query(FILTER, "", 5, 0);

		assertThat(actual).satisfies(uut -> {
					assertThat(uut.values()).isEqualTo(List.of(
							new FrontendValue("", "No Value", null),
							new FrontendValue("a", "Map A", null),
							new FrontendValue("map b", "Map B", null),
							new FrontendValue("map c", "Map C", null),
							new FrontendValue("data a", "data a", "data a")
					));
					assertThat(uut.total()).isEqualTo(13);
				}
		);
	}

	@Test
	@Order(2)
	public void findEmptyTermSecondPage() {
		AutoCompleteResult actual = searchProcessor.query(FILTER, "", 5, 1);

		assertThat(actual).satisfies(uut -> {
					assertThat(uut.values()).isEqualTo(List.of(
							new FrontendValue("b", "Data b", "b"),
							new FrontendValue("data c", "Data C", "data c"),
							new FrontendValue("data d", "data d", "data d"),
							new FrontendValue("","internal", null),
							new FrontendValue("external-null", "external-null", "external-null")
					));
					assertThat(uut.total()).isEqualTo(13);
				}
		);
	}

	@Test
	@Order(2)
	public void findEmptyTermThirdPage() {
		AutoCompleteResult actual = searchProcessor.query(FILTER, "", 5, 2);

		assertThat(actual).satisfies(uut -> {
					assertThat(uut.values()).isEqualTo(List.of(
							new FrontendValue("column c", "column c", "null"),
							new FrontendValue("column ab", "column ab", "null"),
							new FrontendValue("column ba", "column ba", "null")
					));
					assertThat(uut.total()).isEqualTo(13);
				}
		);
	}

	@Test
	@Order(2)
	public void findTerm1() {
		AutoCompleteResult actual = searchProcessor.query(FILTER, "a", 25, 0);

		assertThat(actual).isEqualTo(
				new AutoCompleteResult(
						List.of(
								new FrontendValue("a", "Map A", "null"),
								new FrontendValue("data a", "Data", "data a"),
								new FrontendValue("map b", "Map B", "null"),
								new FrontendValue("map c", "Map C", "null"),
								new FrontendValue("b", "Data b", "b"),
								new FrontendValue("data c", "Data C", "data c"),
								new FrontendValue("data d", "data d", "data d"),
								new FrontendValue("", "internal", null),
								new FrontendValue("external-null", "external-null", "external-null"),
								new FrontendValue("column ab", "column ab", "null"),
								new FrontendValue("column ba", "column ba", "null"),
								new FrontendValue("column c", "column c", "null")
						),
						12
				)
		);
	}

	@Test
	@Order(2)
	public void findTerm2() {
		AutoCompleteResult actual = searchProcessor.query(FILTER, "ab", 25, 0);

		assertThat(actual).isEqualTo(
				new AutoCompleteResult(
						List.of(
								new FrontendValue("column ab", "column ab", null),
								new FrontendValue("column ba", "column ba", null),
								new FrontendValue("a", "Map A", null),
								new FrontendValue("map b", "Map B", null),
								new FrontendValue("map c", "Map C", null),
								new FrontendValue("data a", "Data", "data a"),
								new FrontendValue("b", "Data B", "b"),
								new FrontendValue("data c", "Data C", "data c"),
								new FrontendValue("data d", "data d", "data d"),
								new FrontendValue("column c", "column c", null)
						),
						10
				)
		);
	}

	@Test
	@Order(2)
	public void findPhrase1() {
		AutoCompleteResult actual = searchProcessor.query(FILTER, "column a", 25, 0);

		assertThat(actual).isEqualTo(
				new AutoCompleteResult(
						List.of(
								new FrontendValue("column ab", "column ab", null),
								new FrontendValue("column ba", "column ba", null),
								new FrontendValue("column c", "column c", null)
						),
						3
				)
		);
	}

	@Test
	@Order(2)
	public void findPhrase1LimitPage0() {
		AutoCompleteResult actual = searchProcessor.query(FILTER, "column a", 2, 0);

		assertThat(actual).isEqualTo(
				new AutoCompleteResult(
						List.of(
								new FrontendValue("column ab", "column ab", null),
								new FrontendValue("column ba", "column ba", null)
						),
						3
				)
		);
	}

	@Test
	@Order(2)
	public void findPhrase1LimitPage1() {
		AutoCompleteResult actual = searchProcessor.query(FILTER, "column a", 2, 1);

		assertThat(actual).isEqualTo(
				new AutoCompleteResult(
						List.of(
								new FrontendValue("column c", "column c", null)

						),
						3
				)
		);
	}

	@Test
	@Order(3)
	public void findExactNothing() {

		List<FrontendValue> actual = searchProcessor.findExact(FILTER, "");

		assertThat(actual).isEmpty();
	}

	@Test
	@Order(3)
	public void findExactUnknown() {

		List<FrontendValue> actual = searchProcessor.findExact(FILTER, "z");

		assertThat(actual).isEmpty();
	}

	@Test
	@Order(3)
	public void findExactUppercase() {

		List<FrontendValue> actual = searchProcessor.findExact(FILTER, "MAP A");

		assertThat(actual).containsExactly(new FrontendValue("a", "Map A"));
	}

	@Test
	@Order(4)
	public void checkIndexSkippedValue() throws IOException, SolrServerException {

		SolrClient client = solrConfig.createSearchClient(DATASET_ID.toString());

		// The twin value in the mapping should have been skipped (at multiple points IndexService, SolrSearch, ...) and not be indexed
		SolrQuery query = new SolrQuery("twin");

		QueryResponse response = client.query(query);

		assertThat(response.getResults().getNumFound()).isEqualTo(0);

		client.close();
	}
}

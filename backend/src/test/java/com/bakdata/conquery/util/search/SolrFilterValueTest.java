package com.bakdata.conquery.util.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.LabelMap;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.search.solr.SolrConfig;
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
import com.bakdata.conquery.resources.api.ConceptsProcessor;
import com.bakdata.conquery.resources.api.ConceptsProcessor.AutoCompleteResult;
import com.bakdata.conquery.util.extensions.MockServerExtension;
import com.bakdata.conquery.util.extensions.SolrServerExtension;
import com.bakdata.conquery.util.progressreporter.ProgressReporterImpl;
import com.bakdata.conquery.util.search.solr.SolrBundle;
import com.bakdata.conquery.util.search.solr.SolrProcessor;
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
public class SolrFilterValueTest {


	public static final String MAPPING_PATH = "/shared/mapping.csv";
	public static final DatasetId DATASET_ID = new DatasetId("core1");
	public static final Column SEARCHABLE = createSearchable();
	public static final Environment ENVIRONMENT = new Environment(SolrFilterValueTest.class.getSimpleName());
	public static final ConqueryConfig CONQUERY_CONFIG = new ConqueryConfig();
	@RegisterExtension
	private static final MockServerExtension REF_SERVER = new MockServerExtension(ClientAndServer.startClientAndServer(), SolrFilterValueTest::initRefServer);
	@RegisterExtension
	private static final SolrServerExtension SOLR_SERVER = new SolrServerExtension(DATASET_ID.toString());
	private static final IndexService INDEX_SERVICE = new IndexService(new CsvParserSettings(){{setDelimiterDetectionEnabled(true);setLineSeparatorDetectionEnabled(true);}}, "emptyDefaultLabel");
	public static final SelectFilter<?> FILTER = createFilter();
	public static SolrConfig solrConfig;
	public static SolrProcessor searchProcessor;

	@SneakyThrows(IOException.class)
	public static void initRefServer(ClientAndServer mockServer) {

		try (InputStream inputStream = LoadingUtil.openResource(MAPPING_PATH)) {
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
		solrConfig.getFilterValue().setQueryTemplate("( ${term}^3 *${term}*^2 ${term}~^1 )");
		CONQUERY_CONFIG.setSearch(solrConfig);
		solrBundle.run(CONQUERY_CONFIG, new Environment(SolrFilterValueTest.class.getSimpleName()));
		searchProcessor = solrConfig.createSearchProcessor(ENVIRONMENT, DATASET_ID);
		searchProcessor.start();

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
						"map c", "Map C",
						"e", "Map E" // exists in all sources
				), 0, false);

				final FilterTemplate index = new FilterTemplate(
						new URI("/mapping.csv"),
						"internal",
						"{{external}}",
						""
				);
				index.setName("test1");
				index.setIndexService(INDEX_SERVICE);
				index.setDataset(DATASET_ID);
				index.setConfig(CONQUERY_CONFIG);

				return new ArrayList<>(List.of(labelMap, index, SolrFilterValueTest.SEARCHABLE));
			}

			@Override
			public FilterId getId() {
				return new FilterId(new ConnectorId(new ConceptId(DATASET_ID, "concept"), "connector"), "filter");
			}
		};
	}

	public static Collection<FrontendValue> findExact(SolrProcessor solrProcessor, SelectFilter<?> filter, String searchTerm) {
		return solrProcessor.findExact(filter, List.of(searchTerm)).resolved();
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
				"e", // exists in all sources
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

		Collection<FrontendValue> actual = findExact(searchProcessor, FILTER, "column c");

		assertThat(actual).containsExactly(new FrontendValue("column c", "column c"));
	}

	@Test
	@Order(1)
	public void findExactMap() {
		Collection<FrontendValue> actualLabel = findExact(searchProcessor, FILTER, "Map A");

		assertThat(actualLabel).containsExactly(new FrontendValue("a", "Map A"));


		Collection<FrontendValue> actualValue = findExact(searchProcessor, FILTER, "map a");

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
							new FrontendValue("e", "Map E", null),
							new FrontendValue("map b", "Map B", null),
							new FrontendValue("map c", "Map C", null)
					));
					assertThat(uut.total()).isEqualTo(14);
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
							new FrontendValue("data a", "data a", "data a"),
							new FrontendValue("data c", "Data C", "data c"),
							new FrontendValue("data d", "data d", "data d"),
							new FrontendValue("external-null", "external-null", "external-null")
					));
					assertThat(uut.total()).isEqualTo(14);
				}
		);
	}

	@Test
	@Order(2)
	public void findEmptyTermThirdPage() {
		AutoCompleteResult actual = searchProcessor.query(FILTER, "", 5, 2);

		assertThat(actual).satisfies(uut -> {
					assertThat(uut.values()).isEqualTo(List.of(
							new FrontendValue("","internal", null),
							new FrontendValue("column ab", "column ab", "null"),
							new FrontendValue("column ba", "column ba", "null"),
							new FrontendValue("column c", "column c", "null")
					));
					assertThat(uut.total()).isEqualTo(14);
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
								new FrontendValue("e", "Map E", "null"),
								new FrontendValue("b", "Data b", "b"),
								new FrontendValue("data c", "Data C", "data c"),
								new FrontendValue("data d", "data d", "data d"),
								new FrontendValue("", "internal", null),
								new FrontendValue("external-null", "external-null", "external-null"),
								new FrontendValue("column ab", "column ab", "null"),
								new FrontendValue("column ba", "column ba", "null"),
								new FrontendValue("column c", "column c", "null")
						),
						13
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
								new FrontendValue("e", "Map E", "null"),
								new FrontendValue("data a", "Data", "data a"),
								new FrontendValue("b", "Data B", "b"),
								new FrontendValue("data c", "Data C", "data c"),
								new FrontendValue("data d", "data d", "data d"),
								new FrontendValue("column c", "column c", null)
						),
						11
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

		Collection<FrontendValue> actual = findExact(searchProcessor, FILTER, "");

		assertThat(actual).isEmpty();
	}

	@Test
	@Order(3)
	public void findExactUnknown() {

		Collection<FrontendValue> actual = findExact(searchProcessor, FILTER, "z");

		assertThat(actual).isEmpty();
	}

	@Test
	@Order(3)
	public void findExactUppercase() {

		Collection<FrontendValue> actual = findExact(searchProcessor, FILTER, "MAP A");

		assertThat(actual).containsExactly(new FrontendValue("a", "Map A"));
	}

	@Test
	@Order(3)
	public void findExactMultiple() {

		ConceptsProcessor.ExactFilterValueResult actual = searchProcessor.findExact(FILTER, List.of("MAP A", "z", "Data e"));

		assertThat(actual).usingRecursiveComparison().isEqualTo(new ConceptsProcessor.ExactFilterValueResult(List.of(new FrontendValue("a", "Map A"), new FrontendValue("e", "Data E", "e")),Set.of("z")));
	}

	static final Set<String> MANY_VALUES = Set.of(
			"RwovlXQ1snDlQPmLcMtX",
			"m1QKhB35vjvrw8cWDPIL",
			"tu2LJwmqDSRmVKETE7P4",
			"ZTppQoULmN7DmAoAGbkX",
			"Ljet3MVkeWZEFT4cn8kZ",
			"1gjEY4upDUqKEzTteSf8",
			"nwVhyW0W8RoqIgeZHWII",
			"O1SfaHkMKiAeKlCB3i3X",
			"JCZLQOSbHloHSJCG1V4M",
			"499eumzmxhxaGkr8UjiV",
			"xt1QYhiDPIJvSKDAu9Bm",
			"4Q244beSDOPrAR969i18",
			"JyRV4qyujIpDGOFP23nk",
			"ogGCUNFLKxEZL8Kj2hkR",
			"TP2qyxIJO9yUlN2gkI2R",
			"3njVrHwWoSz2anaSL91W",
			"ekmvmUi7KzLoIKNw4QD6",
			"nCP76KQeCofFa4OSxRpM",
			"CzPW5YkbnLoY58WLbGXM",
			"UxI2wSnBcg4mVTNjZ9Y5",
			"rzOfJ52EEWKuPjgvmRJi",
			"oGlxr9cwD49ExsXh604B",
			"J9EbzvRUcnZwAoSxJhq2",
			"tFVAoBwHoHYqmxHoHbWT",
			"k2VVuS7T3INCBP6J4Hjo",
			"OhDxPFqSNtXxMM9yJdAv",
			"qjKIJ5fRfruj2iBua5qd",
			"hcqeXnjuq0Qq4E8xn8Ib",
			"mGVIPXwXQ444vvLBGvna",
			"MYR4uz7mvdVfLaARDN2T",
			"kdyc0yE3UdjJ3biajnJj",
			"LshtBGUiN6b4X6dVjVrj",
			"fCVDq2WvdynfFGp4R0E4",
			"ossClcGwq5wLRUF8QN1r",
			"9TDxe9FjLo1fAqOMGv0k",
			"yK7vrtg9mbtYYE6K9qwa",
			"osU8sYsgRa6IKO4xevDw",
			"EVtiG3yOFeC32NLKSbcB",
			"102WCDM79qgEfC3rdvrZ",
			"zKaInP73dDDL3Sj0RnU3",
			"3JQp2vezxP5JEA5WaCsG",
			"Gjws7sVd16gJQWIbCSE9",
			"bW6iZn9iQZPfv4sjHpiy",
			"e1FF6FNJ1TVkTK40kK0Y",
			"loVi3ZlegdXKt5KuIWD9",
			"gbf0a1pg33NBhVfeA9Jd",
			"ZxklYSrneuEKWmGr5q0H",
			"8pY9zKWRSXYztTtX2Dsu",
			"uDTnyMyuZ8dTKXtZfwnD",
			"JpLP8mH5d9Bjl8nT7Eot",
			"H8sWEwjn5cNP336XRjBm",
			"FgGZRHNjiNFSjA9jSNpd",
			"JoWsHlySX30ytc4Nrjgp",
			"z9EUPGqJJ2psxI4w5eDx",
			"THAfYCTS7kdoOwdMbem9",
			"tW2dIuFa6xmKk23HW0bB",
			"PhZ9g08JZPF4wJkujckI",
			"16HEc1MMs08jOHElnffy",
			"QhlWwcgkIcgeXWOFLhhE",
			"GFmxh1hjvUW7PWimDZ2f",
			"BYEfxiWXia1NNjLhUc8q",
			"GC3qOEl7VjK7DvQc5J2L",
			"Bn9f8P5jRslBtmKiZPzy",
			"wPRahJuThxpluEIx81lU",
			"9SWXp9YbDFEBlcuVZUaC",
			"yLilVeNK1SCG3ASFVbmq",
			"FT276Es24XGJhUvk5qRx",
			"sjp8ncb9dLkdoL8LLZhv",
			"eDkmN2xZ39mHoTNaKNxE",
			"8GAe0ewJl0zuYZSjEEXR",
			"dYux7hQAPNoptIU2YDW6",
			"OF8pMK4BwiSTAwsZdSaz",
			"IGJA9flaWsVkVjB3goID",
			"RZlMG1eR6ZjTNncj5Oxu",
			"7f5RIlXE0l1PWC8SFjet",
			"phqN9Aj9RWiJJKVBGoac",
			"8CZDNMWZNLavWYudTFzU",
			"6vnLzbizEMz6ONGGIXsh",
			"yyHYfMpGlDP4jGYHwPJD",
			"cZ0C7GKi6fxGLI23aBcp",
			"7jspiPcDFpcTI5eoXOa4",
			"T54xCtrhiuSutpyoyzm9",
			"W9Vo7fweW7TxBeNi0Wkp",
			"CJGkWtCwUQ3VRlt0Fozl",
			"H7UrsRkQO65anpwDSHhL",
			"uAHUDeX0dQH4d5jEBd3t",
			"EagFaADXRsIZ1PJk3D2a",
			"N2Uk836xTQ34jSyi7WNE",
			"3hV3IeIFQoVXy941lzqm",
			"ROc9wJQgwY29jQb9MQJs",
			"EPi6OLT3hfhLmVRgUChO",
			"K4U5dKsvrofd7mY3l5lp",
			"I2MNEbvABg7tZG2tqtx2",
			"qnxDgpgNrK4mid4QoJLz",
			"pTT23LxARdAXVuW0qqCx",
			"EkVVOXSSX0yvgcnngYZ6",
			"lQShCmtrpJwb8rOtKGMQ",
			"pdq1TKljIVxfcakWtpdg",
			"KLM0PRS7rMp1SQUeK8c6",
			"Ho4uzFCzMwrg7t2Pmoxz",
			"xZQDDgfO9HqqAd03nA90",
			"kXpETSkGLtGzJhaQ946K",
			"xcJwuXgmCoawcGuwJLaE",
			"UbO651ujKWbhulALO5LC",
			"cmGfI01YjgMgPPyxwajS",
			"vhekh5AvXYXt2NeDqJPh",
			"jhKi9ZwU0VubFCiGDd2J",
			"y2DWsfpdK3FZ8N4FkTAW",
			"JYj3T6aQjkYTdGirBWr3",
			"cGI2HUgXQwzMMaDhEA6D",
			"XD9UuN053Zj5EGgi6ocm",
			"M64qxq184jKHqk8vlW8J",
			"oo3DkGGKkx57w7ypr8pp",
			"UVHNcXJr93Lyo2pjD94F",
			"k8GMtUxynpstFQ4FNvWi",
			"OyCCPtZ17sYgc9FYLJrS",
			"Yz0wD1llAL3a33txe7lX",
			"Td0WEyqKEelw5E7wajRD",
			"twl0YyWCZ1tDe3P8YvYx",
			"js3FbR48v08Lc6FZD29D",
			"dqDv6kLFdvvcE4Y5s2oz",
			"nafPV1EopdZRibEjxkMU",
			"VutMAZoKihv9pu5DNYXe",
			"Ee7OFTqUxbapi7sBKKQ5",
			"C1wIwOmbwjqTGoNiOEuL",
			"EyNbtpcUTQBAHPoaVku1",
			"c78cAxCXU8gwbvwcpC7F",
			"QxwVWcjHD5hAlcXYw2UU",
			"zz3PhVgI0dWvmKEm5VaD",
			"GrrpSofZ5n3ZutyPJfo7",
			"6DGsMGG2vnupr6zgaAPo",
			"QwCXcdq2utJLdqDg8Bt3",
			"8SFeow3rt7shOQGkwG9Y",
			"lXmRfrosiEGKyqbsyG15",
			"jsSoutG1g5hzrzjR2U2s",
			"Jnau7OBpvvY6VyQRQnaY",
			"RDWZwwFQSGKB5sQ9bGI3",
			"C2qFceAcoks7Oqq8RDT1",
			"4hKjWW1NKEqWoVAB5g9Q",
			"Ia5sNUqzWcUEfQszYVCT",
			"F09xVUfSm7vwjNuf217f",
			"XvohcvcSiQzNne5klOYR",
			"qggTaBquDP6AWbDJ0t0c",
			"VENEl2PjKFcfJd026VJI",
			"4QfGvAM551kTHKbFZHG6",
			"43bEkIQa3c6PoTIzuFIs",
			"PQEa3eVF9OpxaEBlGjpT",
			"Bf911TmARyP6oDWrqATg",
			"SMOjqKYmxHXYvedCA7kc",
			"2O9k7XNkHGIgQ48Ml5Q9",
			"j5p6toahTPxKCIkkMVp0",
			"9pG3k3HhdksD024aBIUJ",
			"zZBCPOzCq6RGHNsBUfcY",
			"pXFhiqulve6oZKFjzH0b",
			"zHo18RaFDSxOo2mUA3lG",
			"e3l8vbkjVogEjKhEOz8D",
			"I7dSYYlGbJDKWpX2eYCE",
			"nSyxFZsdsi6Qv3alJj3N",
			"OKW0RgFDWxrPsuOFBX8s",
			"UtAiuow38wWlfYVOVexE",
			"mXHdVVhXV0H6LfbvxaId",
			"06VyYns8lQwkCrRxXbHe",
			"ZRYCVmYYhpzDnzH2WMLM",
			"nyiGWFlZK0RCZxZhVG8o",
			"m5Rzwx63dFaV2ChBb8wq",
			"bW8ZXsxJ5SuLTC4g9bfF",
			"OWVniAxWgUg2pFDrqDRc",
			"ouZXRPAfLNv8Q9GD7MES",
			"vqwRSUPUhRrPJizL6hCo",
			"JBqIucQoFiZ2VLkz9idU",
			"NKHAXcqzUbmCcL1pSZQC",
			"SqNd2weyL7v8kubJi2xQ",
			"KlmAAUtq6gDYXj5TGf5L",
			"oLgU5vNer0WPfK8ACu4g",
			"80jPGox77ZnnQsV9BqqE",
			"d6Uyy5t6bnw6wtws8YiD",
			"V09NW9x6dztjE8ae6bsg",
			"oRkkc0eWjC5BAhPQXEJr",
			"puo6qpm8IG6Rw3D1mary",
			"U72msLgAbThkScWhPsN2",
			"Wxih33pAxf32hXfVD8C4",
			"gp5zjw4G70PZDx2uaR6c",
			"JETBLaYh0RLNe48PA5o3",
			"iYeOtg57924K801x0AiB",
			"1yQbiNWYhjrqenOMzVoO",
			"Z5qJnace6rp3epQAyOXH",
			"Cdck3TzyEWP5oxT3S99B",
			"J2BI6QhRm4EgrdrFH4cT",
			"OLWaeUN72h8pe3cRtTSS",
			"OUb7kQJBsDFKzDpEdztG",
			"AbhDBDCWnswf3cdklfYQ",
			"LUkSsOHKcheM2scouHce",
			"vB9P1W4rkFiJbth6Nsat",
			"IDzPbQtxYQ8vjLzgNiny",
			"vFh3OxnldRk74QEWXZLa",
			"Xr8n06UbMCT3AC8gZ5Ya",
			"PUcUFAXjFC36Ma6e0EoB",
			"I8P9oDqa2GEIoxLZMFpn",
			"z4h0KXSI2nR0mDuStwpo",
			"kfxY37hayV5z4GpzQfnZ"
	);

	@Test
	@Order(3)
	public void checkExactHandlesManyValues() {

		ConceptsProcessor.ExactFilterValueResult actual = searchProcessor.findExact(FILTER, MANY_VALUES.stream().toList());

		assertThat(actual).usingRecursiveComparison().isEqualTo(new ConceptsProcessor.ExactFilterValueResult(List.of(),MANY_VALUES));
	}

	@Test
	@Order(3)
	public void checkExactFailsValueTooLong() {

		ConceptsProcessor.ExactFilterValueResult actual = searchProcessor.findExact(FILTER, List.of(String.join("",MANY_VALUES)));

		assertThat(actual).usingRecursiveComparison().isEqualTo(new ConceptsProcessor.ExactFilterValueResult(List.of(),MANY_VALUES));
	}

	@Test
	@Order(4)
	public void checkIndexSkippedValue() throws IOException, SolrServerException {

		SolrClient client = solrConfig.createSearchClient(DATASET_ID.toString());

		// The duplicate value in the mapping should have been skipped (at multiple points IndexService, SolrSearch, ...) and not be indexed
		SolrQuery query = new SolrQuery("duplicate");

		QueryResponse response = client.query(query);

		assertThat(response.getResults().getNumFound()).isEqualTo(0);

		client.close();
	}
}

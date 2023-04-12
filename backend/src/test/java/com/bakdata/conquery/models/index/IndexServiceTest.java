package com.bakdata.conquery.models.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.github.powerlibraries.io.In;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
@Slf4j
public class IndexServiceTest {

	private final static Dataset DATASET = new Dataset("dataset");
	private final static ConqueryConfig CONFIG = new ConqueryConfig();
	private final static ClientAndServer REF_SERVER = ClientAndServer.startClientAndServer();
	private final IndexService indexService = new IndexService(new CsvParserSettings());

	@BeforeAll
	@SneakyThrows
	public static void beforeAll() {

		CONFIG.getIndex().setBaseUrl(new URI(String.format("http://localhost:%d/", REF_SERVER.getPort())));

	}

	@AfterAll
	@SneakyThrows
	public static void afterAll() {
		REF_SERVER.stop();
	}

	@Test
	@Order(0)
	void testLoading() throws NoSuchFieldException, IllegalAccessException, URISyntaxException, IOException {
		log.info("Test loading of mapping");

		try (InputStream inputStream = In.resource("/tests/aggregator/FIRST_MAPPED_AGGREGATOR/mapping.csv").asStream()) {
			REF_SERVER.when(request().withPath("/mapping.csv"))
					  .respond(HttpResponse.response().withContentType(new MediaType("text", "csv")).withBody(inputStream.readAllBytes()));
		}

		final MapInternToExternMapper mapper = new MapInternToExternMapper(
				"test1",
				new URI("classpath:/tests/aggregator/FIRST_MAPPED_AGGREGATOR/mapping.csv"),
				"internal",
				"{{external}}",
				"no value"
		);

		final MapInternToExternMapper mapperUrlAbsolute = new MapInternToExternMapper(
				"testUrlAbsolute",
				new URI(String.format("http://localhost:%d/mapping.csv", REF_SERVER.getPort())),
				"internal",
				"{{external}}",
				"no value"
		);

		final MapInternToExternMapper mapperUrlRelative = new MapInternToExternMapper(
				"testUrlRelative",
				new URI("./mapping.csv"),
				"internal",
				"{{external}}",
				"no value"
		);


		injectComponents(mapper, indexService, CONFIG);
		injectComponents(mapperUrlAbsolute, indexService, CONFIG);
		injectComponents(mapperUrlRelative, indexService, CONFIG);

		mapper.init();
		mapperUrlAbsolute.init();
		mapperUrlRelative.init();

		assertThat(mapper.external("int1")).as("Internal Value").isEqualTo("hello");
		assertThat(mapper.external("int2")).as("Internal Value").isEqualTo("int2");

		assertThat(mapperUrlAbsolute.external("int1")).as("Internal Value").isEqualTo("hello");
		assertThat(mapperUrlAbsolute.external("int2")).as("Internal Value").isEqualTo("int2");

		assertThat(mapperUrlRelative.external("int1")).as("Internal Value").isEqualTo("hello");
		assertThat(mapperUrlRelative.external("int2")).as("Internal Value").isEqualTo("int2");

	}

	private static void injectComponents(MapInternToExternMapper mapInternToExternMapper, IndexService indexService, ConqueryConfig config)
			throws NoSuchFieldException, IllegalAccessException {

		final Field indexServiceField = MapInternToExternMapper.class.getDeclaredField(MapInternToExternMapper.Fields.mapIndex);
		indexServiceField.setAccessible(true);
		indexServiceField.set(mapInternToExternMapper, indexService);

		final Field configField = MapInternToExternMapper.class.getDeclaredField(MapInternToExternMapper.Fields.config);
		configField.setAccessible(true);
		configField.set(mapInternToExternMapper, config);

		mapInternToExternMapper.setDataset(DATASET);
	}

	@Test
	@Order(2)
	void testEvictOnMapper()
			throws NoSuchFieldException, IllegalAccessException, URISyntaxException {
		log.info("Test evicting of mapping on mapper");
		final MapInternToExternMapper mapInternToExternMapper = new MapInternToExternMapper(
				"test1",
				new URI("classpath:/tests/aggregator/FIRST_MAPPED_AGGREGATOR/mapping.csv"),
				"internal",
				"{{external}}",
				"no value"
		);

		injectComponents(mapInternToExternMapper, indexService, CONFIG);
		mapInternToExternMapper.init();

		// Before eviction the result should be the same
		assertThat(mapInternToExternMapper.external("int1")).as("Internal Value").isEqualTo("hello");


		MapIndex mappingBeforeEvict = mapInternToExternMapper.getInt2ext();

		indexService.evictCache();

		// Request mapping and trigger reinitialization
		assertThat(mapInternToExternMapper.external("int1")).as("Internal Value").isEqualTo("hello");

		mapInternToExternMapper.init();

		MapIndex mappingAfterEvict = mapInternToExternMapper.getInt2ext();

		// Check that the mapping reinitialized
		assertThat(mappingBeforeEvict).as("Mapping before and after eviction")
									  .isNotSameAs(mappingAfterEvict);
	}

}

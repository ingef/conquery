package com.bakdata.conquery.service.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockserver.model.HttpRequest.request;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.index.MapIndex;
import com.bakdata.conquery.models.index.MapInternToExternMapper;
import com.bakdata.conquery.util.extentions.NamespaceStorageExtension;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.github.powerlibraries.io.In;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.*;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class IndexServiceTest {

	@RegisterExtension
	private static final NamespaceStorageExtension STORAGE_EXTENSION = new NamespaceStorageExtension();

	private static final Dataset DATASET = new Dataset("dataset");
	private static final ConqueryConfig CONFIG = new ConqueryConfig();
	private static final ClientAndServer REF_SERVER = ClientAndServer.startClientAndServer();
	private final IndexService indexService = new IndexService(new CsvParserSettings(), "emptyDefaultLabel");

	@BeforeAll
	@SneakyThrows
	public static void beforeAll() {
		NamespaceStorage namespaceStorage = STORAGE_EXTENSION.getStorage();
		namespaceStorage.updateDataset(DATASET);

		CONFIG.getIndex().setBaseUrl(new URI(String.format("http://localhost:%d/", REF_SERVER.getPort())));

		DATASET.setNsIdResolver(namespaceStorage);
		namespaceStorage.updateDataset(DATASET);

	}

	@AfterAll
	@SneakyThrows
	public static void afterAll() {
		REF_SERVER.stop();
	}

	@Test
	@Order(0)
	void testLoading() throws NoSuchFieldException, IllegalAccessException, URISyntaxException, IOException, ExecutionException, InterruptedException {
		log.info("Test loading of mapping");

		try (InputStream inputStream = In.resource("/tests/aggregator/FIRST_MAPPED_AGGREGATOR/mapping.csv").asStream()) {
			REF_SERVER.when(request().withPath("/mapping.csv"))
					  .respond(HttpResponse.response().withContentType(new MediaType("text", "csv")).withBody(inputStream.readAllBytes()));
		}

		final MapInternToExternMapper mapper = new MapInternToExternMapper(
				"test1",
				new URI("classpath:/tests/aggregator/FIRST_MAPPED_AGGREGATOR/mapping.csv"),
				"internal",
				"{{external}}"
		);

		final MapInternToExternMapper mapperUrlAbsolute = new MapInternToExternMapper(
				"testUrlAbsolute",
				new URI(String.format("http://localhost:%d/mapping.csv", REF_SERVER.getPort())),
				"internal",
				"{{external}}"
		);

		final MapInternToExternMapper mapperUrlRelative = new MapInternToExternMapper(
				"testUrlRelative",
				new URI("./mapping.csv"),
				"internal",
				"{{external}}"
		);


		injectComponents(mapper, indexService);
		injectComponents(mapperUrlAbsolute, indexService);
		injectComponents(mapperUrlRelative, indexService);

		mapper.init();
		mapperUrlAbsolute.init();
		mapperUrlRelative.init();

		// Wait for future
		mapper.getInt2ext().get();
		mapperUrlAbsolute.getInt2ext().get();
		mapperUrlRelative.getInt2ext().get();

		assertThat(mapper.external("int1")).as("Internal Value").isEqualTo("hello");
		assertThat(mapper.external("int2")).as("Internal Value").isEqualTo("int2");

		assertThat(mapperUrlAbsolute.external("int1")).as("Internal Value").isEqualTo("hello");
		assertThat(mapperUrlAbsolute.external("int2")).as("Internal Value").isEqualTo("int2");

		assertThat(mapperUrlRelative.external("int1")).as("Internal Value").isEqualTo("hello");
		assertThat(mapperUrlRelative.external("int2")).as("Internal Value").isEqualTo("int2");

	}

	private static void injectComponents(MapInternToExternMapper mapInternToExternMapper, IndexService indexService)
			throws NoSuchFieldException, IllegalAccessException {

		mapInternToExternMapper.setStorage(STORAGE_EXTENSION.getStorage());

		final Field indexServiceField = MapInternToExternMapper.class.getDeclaredField(MapInternToExternMapper.Fields.mapIndex);
		indexServiceField.setAccessible(true);
		indexServiceField.set(mapInternToExternMapper, indexService);

		final Field configField = MapInternToExternMapper.class.getDeclaredField(MapInternToExternMapper.Fields.config);
		configField.setAccessible(true);
		configField.set(mapInternToExternMapper, CONFIG);

		mapInternToExternMapper.setDataset(DATASET.getId());
	}

	@Test
	@Order(2)
	void testEvictOnMapper()
			throws NoSuchFieldException, IllegalAccessException, URISyntaxException, ExecutionException, InterruptedException {
		log.info("Test evicting of mapping on mapper");
		final MapInternToExternMapper mapInternToExternMapper = new MapInternToExternMapper(
				"test1",
				new URI("classpath:/tests/aggregator/FIRST_MAPPED_AGGREGATOR/mapping.csv"),
				"internal",
				"{{external}}"
		);
		mapInternToExternMapper.setStorage(STORAGE_EXTENSION.getStorage());

		injectComponents(mapInternToExternMapper, indexService);
		mapInternToExternMapper.init();

		// Wait for future
		mapInternToExternMapper.getInt2ext().get();

		// Before eviction the result should be the same
		assertThat(mapInternToExternMapper.external("int1")).as("Internal Value").isEqualTo("hello");


		final MapIndex mappingBeforeEvict = mapInternToExternMapper.getInt2ext().get();

		indexService.evictCache();

		// Request mapping and trigger reinitialization
		assertThat(mapInternToExternMapper.external("int1")).as("Internal Value").isEqualTo("hello");

		mapInternToExternMapper.init();

		final MapIndex mappingAfterEvict = mapInternToExternMapper.getInt2ext().get();

		// Check that the mapping reinitialized
		assertThat(mappingBeforeEvict).as("Mapping before and after eviction")
									  .isNotSameAs(mappingAfterEvict);
	}

	@Test
	void testFailedLoading() throws NoSuchFieldException, IllegalAccessException, URISyntaxException {
		final MapInternToExternMapper mapInternToExternMapper = new MapInternToExternMapper(
				"test1",
				new URI("classpath:/tests/aggregator/FIRST_MAPPED_AGGREGATOR/not_existing_mapping.csv"),
				"internal",
				"{{external}}"
		);

		injectComponents(mapInternToExternMapper, indexService);
		mapInternToExternMapper.init();

		// Wait for future
		assertThatThrownBy(() -> mapInternToExternMapper.getInt2ext().get()).as("Not existent CSV").hasCauseInstanceOf(IllegalStateException.class);


		// Before eviction the result should be the same
		assertThat(mapInternToExternMapper.external("int1")).as("Internal Value").isEqualTo("int1");
	}

}

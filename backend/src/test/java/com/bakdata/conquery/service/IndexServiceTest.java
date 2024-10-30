package com.bakdata.conquery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockserver.model.HttpRequest.request;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.index.MapIndex;
import com.bakdata.conquery.models.index.MapInternToExternMapper;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.bakdata.conquery.util.extensions.MockServerExtension;
import com.github.powerlibraries.io.In;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class IndexServiceTest {
	@RegisterExtension
	private static final MockServerExtension REF_SERVER = new MockServerExtension(ClientAndServer.startClientAndServer(), IndexServiceTest::initRefServer);

	private static final NamespaceStorage NAMESPACE_STORAGE = new NamespaceStorage(new NonPersistentStoreFactory(), IndexServiceTest.class.getName());
	private static final Dataset DATASET = new Dataset("dataset");
	private static final ConqueryConfig CONFIG = new ConqueryConfig();
	private final IndexService indexService = new IndexService(new CsvParserSettings(), "emptyDefaultLabel");

	@SneakyThrows(IOException.class)
	private static void initRefServer(ClientAndServer mockServer) {
		log.info("Test loading of mapping");

		try (InputStream inputStream = In.resource("/tests/aggregator/FIRST_MAPPED_AGGREGATOR/mapping.csv").asStream()) {
			mockServer.when(request().withPath("/mapping.csv"))
					  .respond(HttpResponse.response().withContentType(new MediaType("text", "csv")).withBody(inputStream.readAllBytes()));
		}

	}

	@BeforeAll
	@SneakyThrows
	public static void beforeAll() {

		CONFIG.getIndex().setBaseUrl(new URI(String.format("http://localhost:%d/", REF_SERVER.getPort())));

		NAMESPACE_STORAGE.openStores(null, null);

		DATASET.setNamespacedStorageProvider(NAMESPACE_STORAGE);
		NAMESPACE_STORAGE.updateDataset(DATASET);

	}

	@Test
	@Order(0)
	void testLoading() throws NoSuchFieldException, IllegalAccessException, URISyntaxException, IOException, ExecutionException, InterruptedException {
		log.info("Test loading of mapping");

		try (InputStream inputStream = In.resource("/tests/aggregator/MAPPED/FIRST/mapping.csv").asStream()) {
			REF_SERVER.when(request().withPath("/mapping.csv"))
					  .respond(HttpResponse.response().withContentType(new MediaType("text", "csv")).withBody(inputStream.readAllBytes()));
		}

		final MapInternToExternMapper mapper = new MapInternToExternMapper(
				"test1",
				new URI("classpath:/tests/aggregator/FIRST_MAPPED_AGGREGATOR/mapping.csv"),
				"internal",
				"{{external}}",
				false
		);


		final MapInternToExternMapper mapperUrlAbsolute = new MapInternToExternMapper(
				"testUrlAbsolute",
				new URI(String.format("http://localhost:%d/mapping.csv", REF_SERVER.getPort())),
				"internal",
				"{{external}}",
				false
		);

		final MapInternToExternMapper mapperUrlRelative = new MapInternToExternMapper(
				"testUrlRelative",
				new URI("./mapping.csv"),
				"internal",
				"{{external}}",
				false
		);


		injectComponents(mapper, indexService);
		injectComponents(mapperUrlAbsolute, indexService);
		injectComponents(mapperUrlRelative, indexService);

		mapper.init();
		mapperUrlAbsolute.init();
		mapperUrlRelative.init();

		await().timeout(5, TimeUnit.SECONDS).until(mapper::initialized);
		assertThat(mapper.external("int1")).as("Internal Value").isEqualTo("hello");
		assertThat(mapper.external("int2")).as("Internal Value").isEqualTo("int2");

		await().timeout(5, TimeUnit.SECONDS).until(mapperUrlAbsolute::initialized);
		assertThat(mapperUrlAbsolute.external("int1")).as("Internal Value").isEqualTo("hello");
		assertThat(mapperUrlAbsolute.external("int2")).as("Internal Value").isEqualTo("int2");

		await().timeout(5, TimeUnit.SECONDS).until(mapperUrlRelative::initialized);
		assertThat(mapperUrlRelative.external("int1")).as("Internal Value").isEqualTo("hello");
		assertThat(mapperUrlRelative.external("int2")).as("Internal Value").isEqualTo("int2");

	}

	private static void injectComponents(MapInternToExternMapper mapInternToExternMapper, IndexService indexService)
			throws NoSuchFieldException, IllegalAccessException {

		mapInternToExternMapper.setStorage(NAMESPACE_STORAGE);

		final Field indexServiceField = MapInternToExternMapper.class.getDeclaredField(MapInternToExternMapper.Fields.mapIndex);
		indexServiceField.setAccessible(true);
		indexServiceField.set(mapInternToExternMapper, indexService);

		final Field configField = MapInternToExternMapper.class.getDeclaredField(MapInternToExternMapper.Fields.config);
		configField.setAccessible(true);
		configField.set(mapInternToExternMapper, CONFIG);

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
				"{{external}}",
				false
		);

		injectComponents(mapInternToExternMapper, indexService);
		mapInternToExternMapper.init();

		// Before eviction the result should be the same
		await().timeout(5, TimeUnit.SECONDS).until(mapInternToExternMapper::initialized);
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
}

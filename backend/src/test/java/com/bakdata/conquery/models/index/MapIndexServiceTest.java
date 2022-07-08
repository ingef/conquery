package com.bakdata.conquery.models.index;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.univocity.parsers.csv.CsvParserSettings;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
@Slf4j
public class MapIndexServiceTest {

	private final MapIndexService indexService = new MapIndexService(new CsvParserSettings());

	@Test
	@Order(0)
	void testLoading() throws MalformedURLException, NoSuchFieldException, IllegalAccessException {
		log.info("Test loading of mapping");

		final MapInternToExternMapper mapInternToExternMapper = new MapInternToExternMapper(
				"test1",
				new URL("classpath:/tests/aggregator/FIRST_MAPPED_AGGREGATOR/mapping.csv"),
				"internal",
				"{{external}}"
		);


		injectService(mapInternToExternMapper, indexService);

		mapInternToExternMapper.init();

		assertThat(mapInternToExternMapper.external("int1")).as("Internal Value").isEqualTo("hello");
		assertThat(mapInternToExternMapper.external("int2")).as("Internal Value").isEqualTo("");
	}


	@Test
	@Order(1)
	void testEvictOnService() throws MalformedURLException, ExecutionException, InterruptedException, TimeoutException {
		log.info("Test evicting of mapping on service");
		final CompletableFuture<Map<String, String>> mappingFuture1 = indexService.getMapping(
				new URL("classpath:/tests/aggregator/FIRST_MAPPED_AGGREGATOR/mapping.csv"),
				"internal",
				"{{external}}"
		);

		assertThat(mappingFuture1.get(1, TimeUnit.MINUTES)).isNotNull();

		indexService.evictCache();

		assertThatThrownBy(() -> mappingFuture1.get(1, TimeUnit.MINUTES)).isInstanceOf(CancellationException.class);
		assertThat(mappingFuture1.isCompletedExceptionally()).isTrue();

		final CompletableFuture<Map<String, String>> mappingFuture2 = indexService.getMapping(
				new URL("classpath:/tests/aggregator/FIRST_MAPPED_AGGREGATOR/mapping.csv"),
				"internal",
				"{{external}}"
		);

		assertThat(mappingFuture2.get(1, TimeUnit.MINUTES)).isNotNull();
	}

	@Test
	@Order(2)
	void testEvictOnMapper()
			throws MalformedURLException, ExecutionException, InterruptedException, TimeoutException, NoSuchFieldException, IllegalAccessException {
		log.info("Test evicting of mapping on mapper");
		final MapInternToExternMapper mapInternToExternMapper = new MapInternToExternMapper(
				"test1",
				new URL("classpath:/tests/aggregator/FIRST_MAPPED_AGGREGATOR/mapping.csv"),
				"internal",
				"{{external}}"
		);

		injectService(mapInternToExternMapper, indexService);
		mapInternToExternMapper.init();

		// Before eviction the result should be the same
		assertThat(mapInternToExternMapper.external("int1")).as("Internal Value").isEqualTo("hello");


		final CompletableFuture<Map<String, String>> mappingBeforeEvict = mapInternToExternMapper.getInt2ext();

		indexService.evictCache();

		// Request mapping and trigger reinitialization
		assertThat(mapInternToExternMapper.external("int1")).as("Internal Value").isEqualTo("hello");

		final CompletableFuture<Map<String, String>> mappingAfterEvict = mapInternToExternMapper.getInt2ext();

		// Check that the mapping reinitialized
		assertThat(mappingBeforeEvict).isNotSameAs(mappingAfterEvict);
	}

	private static void injectService(MapInternToExternMapper mapInternToExternMapper, MapIndexService indexService)
			throws NoSuchFieldException, IllegalAccessException {
		final Field indexServiceField = MapInternToExternMapper.class.getDeclaredField(MapInternToExternMapper.Fields.mapIndex);
		indexServiceField.setAccessible(true);
		indexServiceField.set(mapInternToExternMapper, indexService);
	}

}

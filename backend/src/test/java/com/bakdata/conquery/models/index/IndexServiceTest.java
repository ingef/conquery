package com.bakdata.conquery.models.index;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
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
public class IndexServiceTest {

	private final IndexService indexService = new IndexService(new CsvParserSettings());

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

	private static void injectService(MapInternToExternMapper mapInternToExternMapper, IndexService indexService)
			throws NoSuchFieldException, IllegalAccessException {
		final Field indexServiceField = MapInternToExternMapper.class.getDeclaredField(MapInternToExternMapper.Fields.mapIndex);
		indexServiceField.setAccessible(true);
		indexServiceField.set(mapInternToExternMapper, indexService);
	}

}

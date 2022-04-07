package com.bakdata.conquery.models.dictionary;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import org.junit.jupiter.api.Test;


class MapDictionaryTest {

	@Test
	void testSerializationAsList() throws IOException, JSONException {

		MapDictionary map = new MapDictionary(Dataset.PLACEHOLDER, "dictionary");

		map.add("a");
		map.add("b");
		map.add("c");

		final CentralRegistry registry = new CentralRegistry();
		registry.register(Dataset.PLACEHOLDER);

		String json = Jackson.MAPPER.writeValueAsString(map);
		assertThat(json)
				.startsWith("{\"type\":\"MAP_DICTIONARY\",\"dataset\":\"PLACEHOLDER\",\"name\":\"dictionary\",\"id2Value\":[")
				.endsWith("]}");
		SerializationTestUtil
				.forType(MapDictionary.class)
				.registry(registry)
				.test(map);
	}

}

package com.bakdata.conquery.models.dictionary;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.exceptions.JSONException;


class MapDictionaryTest {

	@Test
	void testSerializationAsList() throws IOException, JSONException {
		MapDictionary map =  new MapDictionary();
		map.add("a");
		map.add("b");
		map.add("c");
		
		String json = Jackson.MAPPER.writeValueAsString(map);
		assertThat(json).isEqualTo("[\"MAP_DICTIONARY\",[\"a\",\"b\",\"c\"]]");
		SerializationTestUtil.testSerialization(map, MapDictionary.class);
	}

}

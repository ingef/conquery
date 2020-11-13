package com.bakdata.conquery.models.dictionary;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import org.junit.jupiter.api.Test;


class MapDictionaryTest {

	@Test
	void testSerializationAsList() throws IOException, JSONException {

		MapDictionary map =  new MapDictionary(new DatasetId("dataset"), "dictionary");
		DirectDictionary direct = new DirectDictionary(map);
		direct.add("a");
		direct.add("b");
		direct.add("c");
		
		String json = Jackson.MAPPER.writeValueAsString(map);
		assertThat(json)
			.startsWith("{\"type\":\"MAP_DICTIONARY\",\"name\":\"dictionary\",\"id2Value\":[")
			.endsWith("],\"dataset\":\"dataset\"}");
		SerializationTestUtil
			.forType(MapDictionary.class)
			.test(map);
	}

}

package com.bakdata.conquery.io.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import lombok.Data;


public class JacksonTest {

	@Test
	public void testSingleElementArraySerialization() throws JsonProcessingException {
		String[] arr = new String[] {"singular"};
		assertThat(Jackson.MAPPER.writeValueAsString(arr))
			.isEqualTo("[\"singular\"]");
	}
	
	@Test
	public void testSingleElementListSerialization() throws JsonProcessingException {
		List<String> list = Arrays.asList("singular");
		assertThat(Jackson.MAPPER.writeValueAsString(list))
			.isEqualTo("[\"singular\"]");
	}
	
	@Test
	public void testBiMapSerialization() throws JSONException, IOException {
		BiMap<String, String> map = HashBiMap.create();
		map.put("a", "1");
		map.put("b", "2");
		SerializationTestUtil
			.forType(new TypeReference<BiMap<String, String>>() {})
			.test(map);
	}

	@Test
	public void testInternalOnly() throws JsonProcessingException {
		InternalTestClass test = new InternalTestClass();
		assertThat(Jackson.MAPPER.writeValueAsString(test))
			.isEqualTo("{\"external\":4}");
	}
	
	@Data
	public static class InternalTestClass {
		private int external = 4;
		@InternalOnly
		private int internal = 7;
	}
	
	public static class Marker {}
}

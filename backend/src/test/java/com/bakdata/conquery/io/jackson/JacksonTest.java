package com.bakdata.conquery.io.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.Data;
import org.junit.jupiter.api.Test;


public class JacksonTest {

	@Test
	public void testSingleElementArraySerialization() throws JsonProcessingException {
		String[] arr = new String[] {"singular"};
		assertThat(Mappers.getMapper().writeValueAsString(arr))
			.isEqualTo("[\"singular\"]");
	}
	
	@Test
	public void testSingleElementListSerialization() throws JsonProcessingException {
		List<String> list = Arrays.asList("singular");
		assertThat(Mappers.getMapper().writeValueAsString(list))
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
	public void testNonStrictNumbers() throws JSONException, IOException {
		SerializationTestUtil.forType(Double.class).test(Double.NaN, null);
		SerializationTestUtil.forType(Double.class).test(Double.NEGATIVE_INFINITY, null);
		SerializationTestUtil.forType(Double.class).test(Double.POSITIVE_INFINITY, null);
		SerializationTestUtil.forType(Double.class).test(Double.MAX_VALUE);
		SerializationTestUtil.forType(Double.class).test(Double.MIN_VALUE);
		SerializationTestUtil
				.forType(EntityResult.class)
				.test(
						new MultilineEntityResult(4, List.of(
								new Object[]{0, 1, 2},
								new Object[]{Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}
						)),
						new MultilineEntityResult(4, List.of(
								new Object[]{0, 1, 2},
								new Object[]{null, null, null}
						))
				);
	}

	@Test
	public void testInternalOnly() throws JsonProcessingException {
		InternalTestClass test = new InternalTestClass();
		assertThat(Mappers.getMapper().writeValueAsString(test))
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

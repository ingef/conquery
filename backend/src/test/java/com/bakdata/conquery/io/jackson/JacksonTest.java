package com.bakdata.conquery.io.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;

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

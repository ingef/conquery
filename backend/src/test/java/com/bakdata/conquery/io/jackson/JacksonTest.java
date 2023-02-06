package com.bakdata.conquery.io.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


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

	public static Stream<Arguments> arguments() {
		return Stream
				.of(
						Arguments.of(null, "{\"external\":0,\"apiPersistent\":1,\"internalOnly\":2,\"internalCommunication\":3,\"persistentManager\":5,\"persistentShard\":6,\"persistent\":7,\"api\":8}"),
						Arguments.of(View.InternalCommunication.class, "{\"external\":0,\"internalOnly\":2,\"internalCommunication\":3}"),
						Arguments.of(View.Persistence.Manager.class, "{\"external\":0,\"apiPersistent\":1,\"internalOnly\":2,\"persistentManager\":5,\"persistent\":7}"),
						Arguments.of(View.Persistence.Shard.class, "{\"external\":0,\"internalOnly\":2,\"persistentShard\":6,\"persistent\":7}"),
						Arguments.of(View.Api.class, "{\"external\":0,\"apiPersistent\":1,\"api\":8}")
				);
	}

	@ParameterizedTest
	@MethodSource("arguments")
	public void testViews(Class<? extends View> viewClass, String expected) throws JsonProcessingException {
		InternalTestClass test = new InternalTestClass();
		ObjectWriter writer = Jackson.MAPPER.writerFor(InternalTestClass.class);
		if (viewClass != null) {
			writer = writer.withView(viewClass);
		}
		assertThat(writer.writeValueAsString(test))
				.isEqualTo(expected);
	}
	
	@Data
	public static class InternalTestClass {

		private int external = 0;

		@View.ApiManagerPersistence
		private int apiPersistent = 1;

		@View.Internal
		private int internalOnly = 2;

		@JsonView(View.InternalCommunication.class)
		private int internalCommunication = 3;

		@JsonView(View.Persistence.Manager.class)
		private int persistentManager = 5;

		@JsonView(View.Persistence.Shard.class)
		private int persistentShard = 6;

		@JsonView(View.Persistence.class)
		private int persistent = 7;

		@JsonView(View.Api.class)
		private int api = 8;
	}
	
	public static class Marker {}
}

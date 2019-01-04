package com.bakdata.conquery.io.jackson;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.Data;

public class SerializationBlockTest {
	@Test
	public void testUnmarkedClass() {
		//TODO //FIXME
		/*assertThatThrownBy(() -> {
			Jackson.MAPPER.writeValueAsString(new Unmarked());
		})
		.isInstanceOf(JsonProcessingException.class);*/
	}
	
	@Data
	private static class Unmarked {
		
		private String test = "test";
	}
}

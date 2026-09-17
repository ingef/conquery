package com.bakdata.conquery.io.jackson;

import lombok.Data;
import org.junit.jupiter.api.Test;

public class SerializationBlockTest {
	@Test
	public void testUnmarkedClass() {
		//see #141
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

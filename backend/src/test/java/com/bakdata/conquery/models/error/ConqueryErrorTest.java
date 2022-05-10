package com.bakdata.conquery.models.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import com.bakdata.conquery.io.jackson.Mappers;
import com.bakdata.conquery.models.error.ConqueryError.ExternalResolveFormatError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.jupiter.api.Test;

public class ConqueryErrorTest {
	@Test
	public void errorConvertion() {
		ExternalResolveFormatError error = new ExternalResolveFormatError(5, 6);
		assertThat(error.asPlain()).isEqualTo(new PlainError(error.getId(), "CQ_EXECUTION_CREATION_RESOLVE_EXTERNAL_FORMAT", error.getMessage(), error.getContext()));
	}
	
	@Test
	public void errorDeserialization() throws JsonMappingException, JsonProcessingException {
		PlainError error = Mappers.getMapper().readerFor(PlainError.class).readValue("{\r\n" +
																					 "    \"code\": \"TEST_ERROR\",\r\n" +
																					 "    \"context\": {\r\n" +
																					 "      \"group\": \"group\"\r\n" +
																					 "    },\r\n" +
																					 "    \"id\": \"c8be5f10-1ea8-11eb-8fb8-26885ec43e14\",\r\n" +
																					 "    \"message\": \"group was empty.\"\r\n" +
																					 "  }");
		
		assertThat(error).isEqualTo(new PlainError(UUID.fromString("c8be5f10-1ea8-11eb-8fb8-26885ec43e14"), "TEST_ERROR", "group was empty.", Map.of("group", "group")));
	}

}

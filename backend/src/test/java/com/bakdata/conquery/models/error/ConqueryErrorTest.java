package com.bakdata.conquery.models.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.error.ConqueryError.ExternalResolveFormatError;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

public class ConqueryErrorTest {
	@Test
	public void errorConvertion() {
		final ExternalResolveFormatError error = new ExternalResolveFormatError(5, 6);
		assertThat(error.asPlain()).isEqualTo(new SimpleErrorInfo(error.getId(), "CQ_EXECUTION_CREATION_RESOLVE_EXTERNAL_FORMAT", error.getMessage()));
	}

	@Test
	public void errorDeserialization() throws JsonProcessingException {
		final SimpleErrorInfo error = Jackson.MAPPER.readerFor(SimpleErrorInfo.class).readValue(
				"""
						{\r
						    "code": "TEST_ERROR",\r
						    "context": {\r
						      "group": "group"\r
						    },\r
						    "id": "c8be5f10-1ea8-11eb-8fb8-26885ec43e14",\r
						    "message": "group was empty."\r
						  }""");

		assertThat(error).isEqualTo(new SimpleErrorInfo(UUID.fromString("c8be5f10-1ea8-11eb-8fb8-26885ec43e14"), "TEST_ERROR", "group was empty."));
	}

}

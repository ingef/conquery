package com.bakdata.conquery.models.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.models.error.ConqueryError.ExternalResolveError;
import org.junit.jupiter.api.Test;

public class ConqueryErrorTest {
	@Test
	public void errorConvertion() {
		ExternalResolveError error = new ConqueryError.ExternalResolveError(5, 6);
		assertThat(error.asPlain()).isEqualTo(new PlainError(error.getId(), "CQ_EXECUTION_CREATION_RESOLVE_EXTERNAL", error.getMessage(), error.getContext()));
	}

}

package com.bakdata.conquery.models.identifiable.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PseudomizationTest {
	
	@Test
	void pseudoIdGeneration() {
		AutoIncrementingPseudomizer pseudomizer = new AutoIncrementingPseudomizer(4,2);

		// Id changes from internal to external
		assertThat(pseudomizer.getPseudoId("0")).isEqualTo(EntityPrintId.from(null, null, "anon_0", null));
		
		// Id mapping is constant
		assertThat(pseudomizer.getPseudoId("0")).isEqualTo(pseudomizer.getPseudoId("0"));
		
		// Mapping produces differing external ids
		assertThat(pseudomizer.getPseudoId("1")).isNotEqualTo(pseudomizer.getPseudoId("0"));
		
	}

}

package com.bakdata.conquery.models.identifiable.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PseudomizationTest {
	
	@Test
	void pseudoIdGeneration() {
		AutoIncrementingPseudomizer pseudomizer = new AutoIncrementingPseudomizer(4,2);
		
		String csvId1 = "123";
		String csvId1Copy = "123";
		String csvId2 = "234";
		
		// Id changes from internal to external
		assertThat(pseudomizer.getPseudoId(csvId1)).isEqualTo(EntityPrintId.from(null, null, "anon_0", null));
		
		// Id mapping is constant
		assertThat(pseudomizer.getPseudoId(csvId1)).isEqualTo(pseudomizer.getPseudoId(csvId1Copy));
		
		// Mapping produces differing external ids
		assertThat(pseudomizer.getPseudoId(csvId1)).isNotEqualTo(pseudomizer.getPseudoId(csvId2));
		
	}

}

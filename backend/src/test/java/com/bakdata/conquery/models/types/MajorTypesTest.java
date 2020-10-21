package com.bakdata.conquery.models.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class MajorTypesTest {

	public static MajorTypeId[] reflection() {
		return MajorTypeId.values();
	}
	
	@ParameterizedTest @MethodSource
	public void reflection(MajorTypeId typeId) {
		CType<?,?> type = typeId.createParser().findBestType().getType();
		assertThat(type.getTypeId())
			.isNotNull()
			.isEqualTo(typeId);
	}
}

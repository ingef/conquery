package com.bakdata.conquery.models.events.stores.types;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.parser.MajorTypeId;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class MajorTypesTest {

	public static MajorTypeId[] reflection() {
		return MajorTypeId.values();
	}

	@ParameterizedTest
	@MethodSource
	public void reflection(MajorTypeId typeId) {
		ColumnStore<?> type = typeId.createParser(new ParserConfig()).findBestType();
		assertThat(type.getTypeId())
				.isNotNull()
				.isEqualTo(typeId);
	}
}

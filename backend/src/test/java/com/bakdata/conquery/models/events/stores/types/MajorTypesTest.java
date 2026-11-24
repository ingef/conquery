package com.bakdata.conquery.models.events.stores.types;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.MajorTypeId;
import com.bakdata.conquery.models.preproc.TypeParserSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Assert that every MajorType has a working and associate Parser.
 *
 */
public class MajorTypesTest {

	public static MajorTypeId[] reflection() {
		return MajorTypeId.values();
	}

	@ParameterizedTest
	@MethodSource
	public void reflection(MajorTypeId typeId) {
		TypeParserSupplier.createParser(typeId,new ConqueryConfig()).findBestType();
	}
}

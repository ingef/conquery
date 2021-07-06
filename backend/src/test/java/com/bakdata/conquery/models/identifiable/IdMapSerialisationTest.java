package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;

public class IdMapSerialisationTest {

	public static EntityIdMap createTestPersistentMap() {
		EntityIdMap entityIdMap = new EntityIdMap();

		entityIdMap.addInputMapping(new CsvEntityId("test1"), "a");
		entityIdMap.addOutputMapping(new CsvEntityId("test2"), "c");

		return entityIdMap;
	}

}

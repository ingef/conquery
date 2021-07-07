package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;

public class IdMapSerialisationTest {

	public static EntityIdMap createTestPersistentMap() {
		EntityIdMap entityIdMap = new EntityIdMap();

		entityIdMap.addInputMapping("test1", "a");

		entityIdMap.addOutputMapping("test2", EntityPrintId.from("c"));

		return entityIdMap;
	}

}

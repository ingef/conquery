package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;
import com.bakdata.conquery.models.identifiable.mapping.ExternalId;

public class IdMapSerialisationTest {

	public static EntityIdMap createTestPersistentMap(NamespaceStorage namespaceStorage) {
		EntityIdMap entityIdMap = new EntityIdMap(namespaceStorage);

		entityIdMap.addInputMapping("test1", new ExternalId("id", "a"));

		entityIdMap.addOutputMapping("test2", EntityPrintId.from("c"));

		return entityIdMap;
	}

}

package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingAccessor;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.config.SimpleIdMapping;

public class IdMapSerialisationTest {

	public static PersistentIdMap createTestPersistentMap() {
		PersistentIdMap persistentIdMap = new PersistentIdMap();
		IdMappingConfig mapping = new SimpleIdMapping();
		IdMappingAccessor[] accessors = mapping.getIdAccessors();
		
		persistentIdMap.addMapping(new CsvEntityId("test1"), new ExternalEntityId(new String[] { "a", "b" }), accessors);
		persistentIdMap.addMapping(new CsvEntityId("test2"), new ExternalEntityId(new String[] { "c", "d" }), accessors);
		persistentIdMap.addMapping(new CsvEntityId("test3"), new ExternalEntityId(new String[] { "e", "f" }), accessors);
		persistentIdMap.addMapping(new CsvEntityId("test4"), new ExternalEntityId(new String[] { "g", "h" }), accessors);

		return persistentIdMap;
	}

}

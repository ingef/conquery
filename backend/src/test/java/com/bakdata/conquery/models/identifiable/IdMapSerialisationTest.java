package com.bakdata.conquery.models.identifiable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.identifiable.mapping.SufficientExternalEntityId;

public class IdMapSerialisationTest {

	@Test
	public void base() throws IOException, JSONException {
		Map<CsvEntityId, ExternalEntityId> csvIdToExternalIdMap = new HashMap<>();
		Map<SufficientExternalEntityId, CsvEntityId> externalIdPartCsvIdMap = new HashMap<>();

		csvIdToExternalIdMap.put(new CsvEntityId("test1"), new ExternalEntityId(new String[] { "a", "b" }));
		csvIdToExternalIdMap.put(new CsvEntityId("test2"), new ExternalEntityId(new String[] { "c", "d" }));
		csvIdToExternalIdMap.put(new CsvEntityId("test3"), new ExternalEntityId(new String[] { "e", "f" }));
		csvIdToExternalIdMap.put(new CsvEntityId("test4"), new ExternalEntityId(new String[] { "g", "h" }));

		externalIdPartCsvIdMap.put(new SufficientExternalEntityId(new String[] { "a", "b" }), new CsvEntityId("test1"));
		externalIdPartCsvIdMap.put(new SufficientExternalEntityId(new String[] { "c", "d" }), new CsvEntityId("test2"));
		externalIdPartCsvIdMap.put(new SufficientExternalEntityId(new String[] { "e", "f" }), new CsvEntityId("test3"));
		externalIdPartCsvIdMap.put(new SufficientExternalEntityId(new String[] { "g", "h" }), new CsvEntityId("test4"));

		PersistentIdMap persistentIdMap = new PersistentIdMap(csvIdToExternalIdMap, externalIdPartCsvIdMap);

		SerializationTestUtil.testSerialization(persistentIdMap, PersistentIdMap.class);
	}

}

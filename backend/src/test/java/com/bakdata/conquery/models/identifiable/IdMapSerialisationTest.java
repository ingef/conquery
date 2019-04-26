package com.bakdata.conquery.models.identifiable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.DefaultIdMappingAccessor;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.identifiable.mapping.NoIdMapping;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.identifiable.mapping.SufficientExternalEntityId;

public class IdMapSerialisationTest {

	@Test
	public void base() throws IOException, JSONException {
		Map<CsvEntityId, ExternalEntityId> csvIdToExternalIdMap = new HashMap<>();
		Map<SufficientExternalEntityId, CsvEntityId> externalIdPartCsvIdMap = new HashMap<>();

		csvIdToExternalIdMap.put(new CsvEntityId("test"), new ExternalEntityId(new String[] { "a", "b" }));

		externalIdPartCsvIdMap.put(new SufficientExternalEntityId(
			new DefaultIdMappingAccessor(new NoIdMapping(), new int[] { 0, 1 }),
			new String[] { "a", "b" }), new CsvEntityId("test"));

		PersistentIdMap persistentIdMap = new PersistentIdMap(csvIdToExternalIdMap, externalIdPartCsvIdMap);

		SerializationTestUtil.testSerialization(persistentIdMap, PersistentIdMap.class);
	}

}

package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.identifiable.mapping.SufficientExternalEntityId;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class PersistentIdMapDeserializer extends JsonDeserializer<PersistentIdMap> {

	private static final TypeReference arrayOfMapEntryType = new TypeReference<ArrayList<PersistentIdMapSerializer.ExternalIdMapEntry>>() {};
	private static final TypeReference mapOfCsvToExternalIdType = new TypeReference<Map<CsvEntityId, ExternalEntityId>>() {};

	@Override
	public PersistentIdMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

		Map<CsvEntityId, ExternalEntityId> csvIdToExternalIdMap = p.readValueAs(mapOfCsvToExternalIdType);
		Map<SufficientExternalEntityId, CsvEntityId> externalIdPartCsvIdMap = new HashMap<>();

		List<PersistentIdMapSerializer.ExternalIdMapEntry> mapAsList = p.readValueAs(arrayOfMapEntryType);
		mapAsList.forEach(externalIdMapEntry -> {
			externalIdPartCsvIdMap.put(externalIdMapEntry.getSufficientExternalEntityId(), externalIdMapEntry.getCsvEntityId());
		});
		PersistentIdMap map = new PersistentIdMap();
		map.getCsvIdToExternalIdMap().putAll(csvIdToExternalIdMap);
		map.getExternalIdPartCsvIdMap().putAll(externalIdPartCsvIdMap);
		return map;
	}
}

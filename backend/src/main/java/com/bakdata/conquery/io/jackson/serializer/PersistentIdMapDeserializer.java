package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.identifiable.mapping.UnresolvedEntityId;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class PersistentIdMapDeserializer extends JsonDeserializer<EntityIdMap> {

	private static final TypeReference arrayOfMapEntryType = new TypeReference<ArrayList<PersistentIdMapSerializer.ExternalIdMapEntry>>() {};
	private static final TypeReference mapOfCsvToExternalIdType = new TypeReference<Map<CsvEntityId, EntityPrintId>>() {};

	@Override
	public EntityIdMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

		Map<CsvEntityId, EntityPrintId> csvIdToExternalIdMap = p.readValueAs(mapOfCsvToExternalIdType);
		Map<UnresolvedEntityId, CsvEntityId> externalIdPartCsvIdMap = new HashMap<>();

		List<PersistentIdMapSerializer.ExternalIdMapEntry> mapAsList = p.readValueAs(arrayOfMapEntryType);
		mapAsList.forEach(externalIdMapEntry -> {
			externalIdPartCsvIdMap.put(externalIdMapEntry.getSufficientExternalEntityId(), externalIdMapEntry.getCsvEntityId());
		});
		EntityIdMap map = new EntityIdMap();
		map.getCsvIdToExternalIdMap().putAll(csvIdToExternalIdMap);
		map.getExternalIdPartCsvIdMap().putAll(externalIdPartCsvIdMap);
		return map;
	}
}

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

	@Override
	public PersistentIdMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

		Map<CsvEntityId, ExternalEntityId> csvIdToExternalIdMap = p.readValueAs(new TypeReference<Map<CsvEntityId, ExternalEntityId>>() {});
		Map<SufficientExternalEntityId, CsvEntityId> externalIdPartCsvIdMap = new HashMap<>();

		List<PersistentIdMapSerializer.ExternalIdMapEntry> mapAsList = p.readValueAs(new TypeReference<ArrayList<PersistentIdMapSerializer.ExternalIdMapEntry>>(){});
		mapAsList.forEach(externalIdMapEntry -> {
			externalIdPartCsvIdMap.put(externalIdMapEntry.getSufficientExternalEntityId(),externalIdMapEntry.getCsvEntityId());
		});

		return new PersistentIdMap(csvIdToExternalIdMap, externalIdPartCsvIdMap);
	}
}

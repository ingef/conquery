package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.identifiable.mapping.UnresolvedEntityId;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.Data;

public class PersistentIdMapSerializer extends JsonSerializer<EntityIdMap> {

	@Override
	public void serialize(EntityIdMap value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeObject(value.getCsvIdToExternalIdMap());
		List<ExternalIdMapEntry> mapAsList = new ArrayList<>();

		for (Map.Entry<UnresolvedEntityId, CsvEntityId> entry : value.getExternalIdPartCsvIdMap().entrySet()) {
			UnresolvedEntityId key = entry.getKey();
			CsvEntityId val = entry.getValue();
			mapAsList.add(new ExternalIdMapEntry(key, val));
		}

		gen.writeObject(mapAsList);
	}

	@Data
	public static class ExternalIdMapEntry {
		private final UnresolvedEntityId sufficientExternalEntityId;
		private final CsvEntityId csvEntityId;
	}
}

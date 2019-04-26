package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class PersistentIdMapSerializer extends JsonSerializer<PersistentIdMap> {

	@Override
	public void serialize(PersistentIdMap value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeObject(value.getCsvIdToExternalIdMap());
		List<ExternalIdMapFromListConverter.ExternalIdMapEntry> mapAsList = new ArrayList<>();
		value.getExternalIdPartCsvIdMap().forEach((key, val) -> {
			mapAsList.add(new ExternalIdMapFromListConverter.ExternalIdMapEntry(key, val));
		});
		gen.writeObject(mapAsList);
	}
}

package com.bakdata.conquery.io.jackson.serializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.SufficientExternalEntityId;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.util.StdConverter;

public class ExternalIdMapToListConverter
	extends StdConverter<Map<SufficientExternalEntityId, CsvEntityId>, List<ExternalIdMapFromListConverter.ExternalIdMapEntry>> {

	@Override
	public List<ExternalIdMapFromListConverter.ExternalIdMapEntry> convert(Map<SufficientExternalEntityId, CsvEntityId> value) {
		List<ExternalIdMapFromListConverter.ExternalIdMapEntry> mapAsList = new ArrayList<>();

		value.forEach((key, val) -> {
			mapAsList.add(new ExternalIdMapFromListConverter.ExternalIdMapEntry(key, val));
		});
		return mapAsList;
	}
}

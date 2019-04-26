package com.bakdata.conquery.io.jackson.serializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.SufficientExternalEntityId;
import com.fasterxml.jackson.databind.util.StdConverter;

import lombok.Data;

public class ExternalIdMapFromListConverter
	extends StdConverter<List<ExternalIdMapFromListConverter.ExternalIdMapEntry>, Map<SufficientExternalEntityId, CsvEntityId>> {

	@Override
	public Map<SufficientExternalEntityId, CsvEntityId> convert(List<ExternalIdMapEntry> value) {
		Map<SufficientExternalEntityId, CsvEntityId> externalEntityIdMap = new HashMap<>();
		for (ExternalIdMapEntry entry : value) {
			externalEntityIdMap.put(entry.getSufficientExternalEntityId(), entry.getCsvEntityId());
		}
		return externalEntityIdMap;
	}

	@Data
	public static class ExternalIdMapEntry {
		private final SufficientExternalEntityId sufficientExternalEntityId;
		private final CsvEntityId csvEntityId;
	}
}

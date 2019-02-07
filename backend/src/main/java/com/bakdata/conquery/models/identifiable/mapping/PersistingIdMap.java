package com.bakdata.conquery.models.identifiable.mapping;

import java.util.Map;

import lombok.Data;

@Data
public class PersistingIdMap {
	private Map<CsvId,ExternalId> csvIdToExternalIdMap;
	private Map<ExternalIdPart, CsvId> externalIdPartCsvIdMap;
}

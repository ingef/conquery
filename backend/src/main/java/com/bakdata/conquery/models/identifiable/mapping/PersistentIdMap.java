package com.bakdata.conquery.models.identifiable.mapping;

import java.util.Map;

import lombok.Data;

/**
 *  Mapping from Csv Entity Id to External Entity Id and back from the combinations of Accessor + IDs to the Entity Id.
 */
@Data
public class PersistentIdMap {
	/**
	 * The map from csv entity ids to external entity ids.
	 */
	private final Map<CsvEntityId, ExternalEntityId> csvIdToExternalIdMap;

	/**
	 * The map from external entity ids to csv entity ids.
	 */
	private final Map<SufficientExternalEntityId, CsvEntityId> externalIdPartCsvIdMap;
}

package com.bakdata.conquery.models.identifiable.mapping;

import java.util.Map;

import com.bakdata.conquery.io.jackson.serializer.PersistentIdMapDeserializer;
import com.bakdata.conquery.io.jackson.serializer.PersistentIdMapSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Data;

/**
 * Mapping from Csv Entity Id to External Entity Id and back from the combinations of Accessor + IDs to the Entity Id.
 */
@Data
@JsonSerialize(using = PersistentIdMapSerializer.class)
@JsonDeserialize(using = PersistentIdMapDeserializer.class)
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

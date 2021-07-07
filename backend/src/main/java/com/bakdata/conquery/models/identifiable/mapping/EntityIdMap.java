package com.bakdata.conquery.models.identifiable.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mapping from Csv Entity Id to External Entity Id and back from the
 * combinations of Accessor + IDs to the Entity Id.
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
@Slf4j
public class EntityIdMap {


	/**
	 * The map from csv entity ids to external entity ids.
	 */
	private final Map<String, EntityPrintId> csvIdToExternalIdMap = new HashMap<>();

	/**
	 * The map from external entity ids to csv entity ids.
	 */
	private final Map<String, String> externalIdPartCsvIdMap = new HashMap<>();

	/**
	 * Map an internal id to an external.
	 */
	public EntityPrintId toExternal(String internal) {
		return csvIdToExternalIdMap.get(internal);
	}

	/**
	 * Map an external to an internal id.
	 * @param external
	 */
	public Optional<String> toInternal(String external) {
		return Optional.ofNullable(externalIdPartCsvIdMap.get(external));
	}

	public void addOutputMapping(String csvEntityId, EntityPrintId externalEntityId) {
		final EntityPrintId prior = csvIdToExternalIdMap.put(csvEntityId, externalEntityId);

		if (prior != null && prior.equals(externalEntityId)) {
			log.warn("Duplicate mapping  for {} to {} and {}", csvEntityId, externalEntityId, prior);
		}
	}


	public void addInputMapping(String csvEntityId, String externalEntityId) {

		final String prior = externalIdPartCsvIdMap.put(externalEntityId, csvEntityId);

		if (prior != null && prior.equals(csvEntityId)) {
			log.warn("Duplicate mapping  for {} to {} and {}", externalEntityId, csvEntityId, prior);
		}
	}

}

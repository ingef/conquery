package com.bakdata.conquery.models.identifiable.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.io.jackson.serializer.PersistentIdMapDeserializer;
import com.bakdata.conquery.io.jackson.serializer.PersistentIdMapSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Mapping from Csv Entity Id to External Entity Id and back from the combinations of Accessor + IDs to the Entity Id.
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
@JsonSerialize(using = PersistentIdMapSerializer.class)
@JsonDeserialize(using = PersistentIdMapDeserializer.class)
public class PersistentIdMap implements IdMapper {

	/**
	 * The map from csv entity ids to external entity ids.
	 */
	private final Map<CsvEntityId, ExternalEntityId> csvIdToExternalIdMap = new HashMap<>();

	/**
	 * The map from external entity ids to csv entity ids.
	 */
	private final Map<SufficientExternalEntityId, CsvEntityId> externalIdPartCsvIdMap = new HashMap<>();

	@Override
	public ExternalEntityId toExternal(CsvEntityId internal) {
		return csvIdToExternalIdMap.get(internal);
	}

	@Override
	public CsvEntityId toInternal(SufficientExternalEntityId external) {
		return externalIdPartCsvIdMap.get(external);
	}

	@Override
	public void addMapping(CsvEntityId internal, ExternalEntityId external, IdMappingAccessor[] idMappingAccessors) {
		// Map internal to external
		csvIdToExternalIdMap.put(internal, external);
		// Map the inverse with different unique shapes
		for (IdMappingAccessor accessor : idMappingAccessors)
		externalIdPartCsvIdMap.put(
				new SufficientExternalEntityId(accessor.extract(external.getExternalId())),
			internal);
	}

	/**
	 * Checks if the given CsvContent produces unique results in perspective to
	 * all IdMappingAccessors.
	 * 
	 * @param data
	 *            Map of CsvEntityId to External Ids as read from the given CSV.
	 * @throws IllegalArgumentException
	 *             if the inserted Ids are not unique.
	 */
	public void checkIntegrity(Collection<IdMappingAccessor> idAccessor) {
		// check that each idMappingAccessor leads to at most one tuple
		for (IdMappingAccessor idMappingAccessor : idAccessor) {
			long distinctSize = csvIdToExternalIdMap.values().stream()
				.map(p -> idMappingAccessor.extract(p.getExternalId()))
				.distinct().count();
			// check if we still have the same size as before
			if (distinctSize != csvIdToExternalIdMap.size()) {
				throw new IllegalArgumentException(
					"The inserted IDs are not unique respective to the idMapping Accessor "
						+ idMappingAccessor);
			}
		}
	}

}

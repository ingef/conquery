package com.bakdata.conquery.models.identifiable.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mapping from Csv Entity Id to External Entity Id and back from the
 * combinations of Accessor + IDs to the Entity Id.
 */
@Getter
@EqualsAndHashCode
@Slf4j
@NoArgsConstructor
public class EntityIdMap {



	/**
	 * The map from csv entity ids to external entity ids.
	 */
	private final Map<String, EntityPrintId> internalToPrint = new HashMap<>();

	/**
	 * The map from external entity ids to csv entity ids.
	 */
	private final Map<ExternalId, String> external2Internal = new HashMap<>();


	@Data
	protected static class Container {
		private final Collection<Map.Entry<ExternalId,String>> external2Internal;
		private final Map<String, EntityPrintId> internalToPrint;
	}

	@JsonCreator
	protected EntityIdMap fromContainer(Container container) {
		final EntityIdMap out = new EntityIdMap();

		for (Map.Entry<ExternalId, String> entry : container.getExternal2Internal()) {
			out.getExternal2Internal().put(entry.getKey(), entry.getValue());
		}

		out.getInternalToPrint().putAll(container.getInternalToPrint());

		return out;
	}

	@JsonValue
	protected Container getContainer() {
		return new Container(external2Internal.entrySet(), internalToPrint);
	}

	/**
	 * Map an internal id to an external.
	 */
	public EntityPrintId toExternal(String internal) {
		return internalToPrint.get(internal);
	}

	/**
	 * Resolve an external to an internal id.
	 * @param external
	 */
	public Optional<String> toInternal(String... external) {
		return Optional.ofNullable(external2Internal.get(new ExternalId(external)));
	}

	public void addOutputMapping(String csvEntityId, EntityPrintId externalEntityId) {
		final EntityPrintId prior = internalToPrint.put(csvEntityId, externalEntityId);

		if (prior != null && prior.equals(externalEntityId)) {
			log.warn("Duplicate mapping  for {} to {} and {}", csvEntityId, externalEntityId, prior);
		}
	}


	public void addInputMapping(String csvEntityId, String... externalEntityId) {

		final String prior = external2Internal.put(new ExternalId(externalEntityId), csvEntityId);

		if (prior != null && prior.equals(csvEntityId)) {
			log.warn("Duplicate mapping  for {} to {} and {}", externalEntityId, csvEntityId, prior);
		}
	}

	@Data
	private static class ExternalId {
		private final String[] parts;
	}

}

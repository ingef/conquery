package com.bakdata.conquery.models.identifiable.mapping;

import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.ColumnConfig;
import com.fasterxml.jackson.annotation.*;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapping from uploaded {@link ExternalId} for resolving in {@link com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal}, and also for printing with {@link EntityPrintId}.
 */
@Getter
@EqualsAndHashCode
@Slf4j
@NoArgsConstructor
public class EntityIdMap {

	@Setter
	@JsonIgnore
	@EqualsAndHashCode.Exclude
	@JacksonInject(useInput = OptBoolean.FALSE)
	private NamespaceStorage storage;

	/**
	 * The map from csv entity ids to external entity ids.
	 */
	@JsonIgnore
	private final Map<String, EntityPrintId> internalToPrint = new HashMap<>();

	/**
	 * The map from external entity ids to csv entity ids.
	 */
	@JsonIgnore
	private final Map<ExternalId, String> external2Internal = new HashMap<>();

	/**
	 * Read incoming CSV-file extracting Id-Mappings for {@link ExternalId} and {@link EntityPrintId}.
	 */
	public static EntityIdMap generateIdMapping(CsvParser parser, List<ColumnConfig> mappers) {

		EntityIdMap mapping = new EntityIdMap();

		Record record;


		while ((record = parser.parseNextRecord()) != null) {
			List<String> idParts = new ArrayList<>(mappers.size());

			final String id = record.getString("id");

			for (ColumnConfig columnConfig : mappers) {

				final String otherId = record.getString(columnConfig.getField());

				// Collect printable parts into id
				if (columnConfig.isPrint()) {
					idParts.add(otherId);
				}

				if (otherId == null) {
					continue;
				}

				final ExternalId transformed = columnConfig.read(otherId);

				mapping.addInputMapping(id, transformed);
			}

			mapping.addOutputMapping(id, new EntityPrintId(idParts.toArray(new String[0])));
		}

		return mapping;
	}


	/**
	 * Helper class for serialization.
	 */
	@Data
	@Getter
	@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
	public static class Container {
		private final List<ExternalId> keys;
		private final List<String> values;
		private final Map<String, EntityPrintId> internalToPrint;
	}

	/**
	 * Constructor to deserialize from {@link Container}.
	 */
	@JsonCreator
	public EntityIdMap(Container container) {

		for (int index = 0; index < container.keys.size(); index++) {
			getExternal2Internal().put(container.keys.get(index), container.values.get(index));
		}

		getInternalToPrint().putAll(container.internalToPrint);
	}

	/**
	 * JsonValue to Serialize as {@link Container}, as Jackson cannot handle complex classes as Map-keys (ie {@link ExternalId}.
	 */
	@JsonValue
	private Container getContainer() {
		final List<ExternalId> keys = new ArrayList<>(external2Internal.size());
		final List<String> values = new ArrayList<>(external2Internal.size());

		external2Internal.forEach((key, value) -> {
			keys.add(key);
			values.add(value);
		});

		return new Container(keys, values, internalToPrint);
	}

	/**
	 * Map an internal id to an external.
	 */
	public EntityPrintId toExternal(String internal) {
		return internalToPrint.get(internal);
	}

	/**
	 * Resolve external ID to Entity Id.
	 * <p>
	 * Return -1 when not resolved.
	 */
	public String resolve(ExternalId key) {
		final String value = external2Internal.get(key);

		if (value != null) {
			return value;
		}

		// Maybe we can find them directly in the dictionary?
		if (storage.getEntityBucket(key.getId()).isPresent()) {
			return key.getId();
		}

		return null;
	}

	public void addOutputMapping(String csvEntityId, EntityPrintId externalEntityId) {
		final EntityPrintId prior = internalToPrint.put(csvEntityId, externalEntityId);

		if (prior != null && prior.equals(externalEntityId)) {
			log.warn("Duplicate mapping  for {} to {} and {}", csvEntityId, externalEntityId, prior);
		}
	}


	public void addInputMapping(String csvEntityId, ExternalId externalEntityId) {

		final String prior = external2Internal.put(externalEntityId, csvEntityId);

		if (prior != null && prior.equals(csvEntityId)) {
			log.warn("Duplicate mapping  for {} to {} and {}", externalEntityId, csvEntityId, prior);
		}
	}

}

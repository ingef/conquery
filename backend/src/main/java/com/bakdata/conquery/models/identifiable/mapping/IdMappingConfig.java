package com.bakdata.conquery.models.identifiable.mapping;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.apiv1.query.concept.specific.external.DateColumn;
import com.bakdata.conquery.apiv1.query.concept.specific.external.DateFormat;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ColumnConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@NoArgsConstructor
@Setter
@Getter
public class IdMappingConfig {


	public final Map<String, DateFormat> dateFormats = Map.of(
			DateColumn.DateSet.HANDLE, DateFormat.DATE_SET,
			DateColumn.DateRange.HANDLE, DateFormat.DATE_RANGE,
			DateColumn.EventDate.HANDLE, DateFormat.EVENT_DATE,
			DateColumn.StartDate.HANDLE, DateFormat.START_END_DATE,
			DateColumn.EndDate.HANDLE, DateFormat.START_END_DATE
	);

	private List<ColumnConfig> mappers = List.of(
			ColumnConfig.builder()
						.name("ID")
						.mapping(ColumnConfig.Mapping.builder()
													 .field("id")
													 .resolvable(true)
													 .build())
						.build());

	private OutputMapper outputMapper = new OutputMapper();

	/**
	 * Read incoming CSV-file extracting Id-Mappings for in and Output.
	 */
	public EntityIdMap generateIdMapping(CsvParser parser) {

		EntityIdMap mapping = new EntityIdMap();

		Record record;

		while ((record = parser.parseNextRecord()) != null) {
			final String id = record.getString("id");

			for (ColumnConfig columnConfig : mappers) {

				final String otherId = record.getString(columnConfig.getMapping().getField());
				final EntityIdMap.ExternalId transformed = columnConfig.read(otherId);

				mapping.addInputMapping(id, transformed);
			}

			mapping.addOutputMapping(id, outputMapper.extractOutputId(record));
		}

		return mapping;
	}

	/**
	 * Headers for Output CSV.
	 */
	@JsonIgnore
	public List<String> getPrintIdFields() {
		return List.of(outputMapper.getHeaders());
	}

	/**
	 * Is called once before a mapping is used before a query result is created to
	 * allow the mapping to have state information.
	 */
	public IdMappingState initToExternal(User user, ManagedExecution<?> execution) {
		// This mapping does not need a per-query state, so we return an immutable empty map.
		return null;
	}

	/**
	 * Converts the internal ID to the an external.
	 */
	public EntityPrintId toExternal(String csvEntityId, Namespace namespace, IdMappingState state, EntityIdMap mapping) {
		// The state may be uses by implementations of this class

		if (mapping == null) {
			return EntityPrintId.from(csvEntityId);
		}

		EntityPrintId externalEntityId = mapping.toExternal(csvEntityId);

		if (externalEntityId == null) {
			return EntityPrintId.from(csvEntityId);
		}

		return externalEntityId;
	}

	public ColumnConfig getIdMapper(String name) {
		return mappers.stream()
					  .filter(mapper -> mapper.getName().equals(name)) //TODO use map
					  .findFirst()
					  .orElse(null);
	}

	public int getIdIndex(List<String> format) {
		for (int index = 0; index < format.size(); index++) {
			final String current = format.get(index);

			if (mappers.stream().map(ColumnConfig::getName).anyMatch(current::equals)) {
				return index;
			}
		}

		return -1;
	}




	@CPSBase
	@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
	@NoArgsConstructor
	public static class OutputMapper {
		@JsonIgnore
		public String[] getHeaders() {
			return new String[]{"result"};
		}

		public EntityPrintId extractOutputId(Record record) {
			return new EntityPrintId(new String[]{record.getString("id")});
		}
	}
}

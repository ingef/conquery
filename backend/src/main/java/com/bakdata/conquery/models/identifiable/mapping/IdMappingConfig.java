package com.bakdata.conquery.models.identifiable.mapping;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.apiv1.query.concept.specific.external.DateColumn;
import com.bakdata.conquery.apiv1.query.concept.specific.external.DateFormat;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.DateReader;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Strings;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


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

	private List<InputMapper> mappers = List.of(new InputMapper("ID", "id", "0", -1, Collections.emptyMap(), Collections.emptyMap()));

	private OutputMapper outputMapper = new OutputMapper();

	/**
	 * Read incoming CSV-file extracting Id-Mappings for in and Output.
	 */
	public EntityIdMap generateIdMapping(CsvParser parser) {

		EntityIdMap mapping = new EntityIdMap();

		Record record;

		while ((record = parser.parseNextRecord()) != null) {
			final String id = record.getString("id");

			for (InputMapper mapper : mappers) {
				final String otherId = record.getString(mapper.getField());
				final String transformed = mapper.read(otherId);

				mapping.addInputMapping(id, mapper.getName(), transformed);
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

	public InputMapper getIdMapper(String name) {
		return mappers.stream().filter(mapper -> mapper.getName().equals(name)).findFirst().orElse(null);
	}

	public int getIdIndex(List<String> format) {
		for (int index = 0; index < format.size(); index++) {
			final String current = format.get(index);

			if (mappers.stream().map(InputMapper::getName).anyMatch(current::equals)) {
				return index;
			}
		}

		return -1;
	}


	public Int2ObjectMap<CDateSet> readDates(String[][] values, List<String> format, DateReader dateReader) {
		Int2ObjectMap<CDateSet> out = new Int2ObjectAVLTreeMap<>();

		DateFormat dateFormat = null;


		IntList dateColumns = new IntArrayList(format.size());

		for (int col = 0; col < format.size(); col++) {
			String desc = format.get(col);

			dateFormat = resolveDateFormat(desc);

			if (dateFormat == null) {
				continue;
			}

			dateColumns.add(col);
		}

		dateFormat = dateFormat == null ? DateFormat.ALL : dateFormat;
		final int[] datePositions = dateColumns.toIntArray();

		for (int row = 1; row < values.length; row++) {
			try {
				final CDateSet dates = dateFormat.readDates(datePositions, values[row], dateReader);

				if (dates == null) {
					continue;
				}

				out.put(row, dates);
			}
			catch (Exception e) {
				log.warn("Failed to parse Date from {}", row, e);
			}
		}

		return out;
	}

	public DateFormat resolveDateFormat(String desc) {
		return dateFormats.get(desc);
	}

	@Data
	public static class InputMapper {
		private final String name;

		private final String field;

		private final String pad;
		private final int length;

		private final Map<String, String> label;
		private final Map<String, String> description;

		public String read(String value) {
			if (Strings.isNullOrEmpty(value)) {
				return null;
			}

			if (length == -1) {
				return value;
			}

			return StringUtils.leftPad(value, length, pad);
		}
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

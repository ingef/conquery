package com.bakdata.conquery.models.identifiable.mapping;

import java.util.*;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@CPSBase
@RequiredArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public abstract class IdMappingConfig {

	public EntityIdMap generateIdMapping(CsvParser parser) throws IllegalArgumentException {

		EntityIdMap mapping = new EntityIdMap();

		//TODO Check headers match parser.getContext().headers();

		Record record;

		while((record = parser.parseNextRecord()) != null){
			final String id = record.getString("id");

			final CsvEntityId csvEntityId = new CsvEntityId(id);

			processRecord(record, csvEntityId, mapping);
		}

		//TODO mapping.checkIntegrity(Arrays.asList(getIdAccessors()));

		return mapping;
	}

	protected abstract void processRecord(Record record, CsvEntityId id,  EntityIdMap mapping);

	@JsonIgnore
	public abstract List<String> getPrintIdFields();

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
	public EntityPrintId toExternal(CsvEntityId csvEntityId, Namespace namespace, IdMappingState state, EntityIdMap mapping) {
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

}

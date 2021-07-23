package com.bakdata.conquery.models.identifiable.mapping;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ColumnConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.worker.Namespace;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@NoArgsConstructor(access = AccessLevel.NONE)
@Setter
@Getter
public class IdMappingConfig {


	/**
	 * Is called once before a mapping is used before a query result is created to
	 * allow the mapping to have state information.
	 */
	public static IdMappingState initToExternal(User user, ManagedExecution<?> execution) {
		// This mapping does not need a per-query state, so we return an immutable empty map.
		return null;
	}

	/**
	 * Converts the internal ID to the an external.
	 */
	public static EntityPrintId toExternal(String csvEntityId, Namespace namespace, IdMappingState state, EntityIdMap mapping) {
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

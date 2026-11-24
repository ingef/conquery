package com.bakdata.conquery.sql.mapping;

import java.util.Set;

import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import lombok.NonNull;
import org.jooq.Record;
import org.jooq.Table;

/** Fully resolved concept-element selection applied to one connector source. */
public record ConceptIdMappingSelection(
		@NonNull ConceptIdMappingSource source,
		Set<Integer> includedLocalIds,
		boolean includesRoot,
		boolean resolveConceptIds,
		int rootLocalId
) {

	public ConceptIdMappingSelection {
		includedLocalIds = Set.copyOf(includedLocalIds);
	}

	public Table<Record> selectedConcepts(Table<Record> connectorTable, TranslatorDialect dialect) {
		return source.selectedConcepts(
				connectorTable,
				includedLocalIds,
				includesRoot,
				resolveConceptIds,
				dialect
		);
	}
}

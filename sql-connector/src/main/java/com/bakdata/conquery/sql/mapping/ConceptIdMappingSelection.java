package com.bakdata.conquery.sql.mapping;

import java.util.Objects;
import java.util.Set;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import org.jooq.Record;
import org.jooq.Table;

/** Fully resolved concept-element selection applied to one connector source. */
public record ConceptIdMappingSelection(
		ConceptIdMappingSource source,
		Set<Integer> includedLocalIds,
		boolean includesRoot,
		boolean resolveConceptIds,
		int rootLocalId
) {

	public ConceptIdMappingSelection {
		Objects.requireNonNull(source, "source");
		includedLocalIds = Set.copyOf(includedLocalIds);
	}

	public Table<Record> selectedConcepts(Table<Record> connectorTable, CompilerDialect dialect) {
		return source.selectedConcepts(
				connectorTable,
				includedLocalIds,
				includesRoot,
				resolveConceptIds,
				dialect
		);
	}
}

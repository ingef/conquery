package com.bakdata.conquery.sql.mapping;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

/** SQL source built by joining connector values to their most-specific concept element. */
public record ConceptIdMappingSource(
		ConceptIdMappingTable mapping,
		Map<String, Field<?>> extractors
) {

	public ConceptIdMappingSource {
		Objects.requireNonNull(mapping, "mapping");
		extractors = Map.copyOf(extractors);
	}

	public Table<Record> selectedConcepts(
			Table<Record> connectorTable,
			Set<Integer> includedLocalIds,
			boolean includesRoot,
			boolean resolveConceptIds,
			CompilerDialect dialect
	) {
		if (includesRoot && !resolveConceptIds) {
			return connectorTable;
		}

		Condition conceptFilterCondition = includedLocalIds.isEmpty()
				? DSL.noCondition()
				: mapping.resolvedId().in(includedLocalIds);

		return connectorTable.innerJoin(mapping.table()).on(joinCondition(dialect).and(conceptFilterCondition));
	}

	public TableLike<? extends Record> resolvedConceptIds(Table<Record> connectorTable, CompilerDialect dialect) {
		return connectorTable.leftJoin(mapping.table()).on(joinCondition(dialect));
	}

	public Field<Integer> resolvedIdOrRoot(int rootLocalId) {
		return DSL.coalesce(mapping.resolvedId(), DSL.inline(rootLocalId));
	}

	public Condition joinCondition(CompilerDialect dialect) {
		return joinCondition(dialect.unconditionalJoinCondition());
	}

	public Condition joinCondition(Condition unconditionalJoinCondition) {
		Condition condition = DSL.noCondition();
		for (Field<?> keyField : mapping.keyFields()) {
			Field<?> extractor = extractors.get(keyField.getName());
			if (extractor == null) {
				throw new IllegalArgumentException(
						"No connector expression for concept mapping field `%s`".formatted(keyField.getName())
				);
			}
			condition = condition.and(mapping.mappingField(keyField).eq((Field) extractor));
		}
		return condition.equals(DSL.noCondition()) ? unconditionalJoinCondition : condition;
	}
}

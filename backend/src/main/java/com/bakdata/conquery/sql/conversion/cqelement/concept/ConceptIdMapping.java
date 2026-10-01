package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.*;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.mapping.ConceptIdMappingResolver;
import com.bakdata.conquery.sql.mapping.ConceptIdMappingSource;
import com.bakdata.conquery.sql.mapping.ConceptIdMappingTable;
import lombok.Data;
import org.jooq.*;
import org.jooq.Record;

import static org.jooq.impl.DSL.*;

/**
 * Description of the physical lookup table that maps connector values to their most specific concept element.
 */
@Data
public final class ConceptIdMapping {

	public static final String RESOLVED_ID_COLUMN = ConceptIdMappingTable.RESOLVED_ID_COLUMN;

	private final TreeConcept concept;
	private final SqlFunctionProvider functionProvider;
	private final Name tableName;
	private final List<Field<?>> keyFields;
	private final List<RowN> rows;

	public ConceptIdMapping(TreeConcept concept, SqlFunctionProvider functionProvider) {
		this.concept = concept;
		this.functionProvider = functionProvider;
		CTConditionContext context = CTConditionContext.forJoinTables(functionProvider);
		List<CTCondition.ConceptConditions> expressions = collectAllExpressions(concept, null, context);
		this.tableName = tableName(concept.getId());
		ConceptIdMappingTable mappingTable = ConceptIdMappingResolver.resolve(
				concept.getId().toString(), tableName, resolveExpressions(expressions)
		);
		this.keyFields = mappingTable.keyFields();
		this.rows = mappingTable.rows();
	}

	public static Name tableName(ConceptId conceptId) {
		return name("%s_ids".formatted(conceptId));
	}

	private static List<ConceptIdMappingResolver.MappingExpression> resolveExpressions(
			List<CTCondition.ConceptConditions> expressions
	) {
		return expressions.stream().map(expression -> new ConceptIdMappingResolver.MappingExpression(
				expression.conceptElement().getId().toString(),
				expression.conceptElement().getLocalId(),
				expression.conceptElement().getDepth(),
				expression.conditions().entrySet().stream().collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> new ConceptIdMappingResolver.FieldCondition(
								entry.getValue().extractor(), entry.getValue().params()
						)
				))
		)).toList();
	}

	private static List<CTCondition.ConceptConditions> collectAllExpressions(
			ConceptElement<?> current,
			CTCondition.ConceptConditions parentExpression,
			CTConditionContext context
	) {
		CTCondition.ConceptConditions currentExpression = switch (current) {
			case TreeConcept concept -> new CTCondition.ConceptConditions(concept, Collections.emptyMap());
			case ConceptTreeChild child -> child.getCondition().buildExpression(context, current).and(parentExpression);
			case null, default -> throw new IllegalStateException();
		};

		List<CTCondition.ConceptConditions> expressions = new ArrayList<>();
		expressions.add(currentExpression);
		for (ConceptTreeChild child : current.getChildren()) {
			expressions.addAll(collectAllExpressions(child, currentExpression, context));
		}
		return expressions;
	}

	public Table<Record> table() {
		return mappingTable().table();
	}

	public Field<Integer> resolvedId() {
		return mappingTable().resolvedId();
	}

	public List<Field<?>> tableFields() {
		return mappingTable().tableFields();
	}

	public ConceptIdMappingTable mappingTable() {
		return new ConceptIdMappingTable(tableName, keyFields, rows);
	}

	public ConceptIdMappingSource source(Connector connector) {
		CTConditionContext context = CTConditionContext.forConnector(connector, functionProvider);
		List<CTCondition.ConceptConditions> expressions = collectAllExpressions(concept, null, context);
		Map<String, Field<?>> extractors = ConceptIdMappingResolver.collectExtractors(resolveExpressions(expressions));
		return new ConceptIdMappingSource(mappingTable(), extractors);
	}

	public Condition joinCondition(Connector connector) {
		return source(connector).joinCondition(functionProvider.unconditionalJoinCondition());
	}

	/**
	 * The mapping table stores the most-specific match. A selected parent therefore accepts every mapped element whose prefix it matches.
	 */
	public Set<Integer> includedLocalIds(List<ConceptElement<?>> selectedElements) {
		return concept.getAllChildren()
				.filter(candidate -> selectedElements.stream().anyMatch(selected -> selected.matchesPrefix(candidate.getPrefix())))
				.map(ConceptElement::getLocalId)
				.collect(Collectors.toSet());
	}

	public boolean includesRoot(List<ConceptElement<?>> selectedElements) {
		return selectedElements.stream().anyMatch(TreeConcept.class::isInstance);
	}
}

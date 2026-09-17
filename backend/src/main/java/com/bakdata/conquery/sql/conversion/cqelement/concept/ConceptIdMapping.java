package com.bakdata.conquery.sql.conversion.cqelement.concept;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.SQLDataType.VARCHAR;

import java.util.*;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.google.common.collect.Sets;
import lombok.Data;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;

/**
 * Description of the physical lookup table that maps connector values to their most specific concept element.
 */
@Data
public final class ConceptIdMapping {

	public static final String RESOLVED_ID_COLUMN = "resolved_id";

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
		this.keyFields = collectKeyFields(expressions);
		this.rows = expressionsToRows(concept, expressions, keyFields);
		this.tableName = tableName(concept.getId());
	}

	public static Name tableName(ConceptId conceptId) {
		return name("%s_ids".formatted(conceptId));
	}

	private static List<Field<?>> collectKeyFields(List<CTCondition.ConceptConditions> expressions) {
		Map<String, Field<?>> fields = new TreeMap<>();
		for (CTCondition.ConceptConditions expression : expressions) {
			for (Field<?> field : expression.conditions().keySet()) {
				fields.merge(field.getName(), field, ConceptIdMapping::mergeFieldType);
			}
		}
		return List.copyOf(fields.values());
	}

	private static Field<?> mergeFieldType(Field<?> left, Field<?> right) {
		DataType<?> leftType = left.getDataType();
		DataType<?> rightType = right.getDataType();
		if (leftType.isString() && rightType.isString()) {
			int length = Math.max(leftType.length(), rightType.length());
			return length > 0 ? field(left.getUnqualifiedName(), VARCHAR(length)) : field(left.getUnqualifiedName(), VARCHAR);
		}
		if (!leftType.getType().equals(rightType.getType())) {
			throw new IllegalArgumentException(
					"Concept mapping field `%s` is used with incompatible types %s and %s"
							.formatted(left.getName(), leftType, rightType)
			);
		}
		return left;
	}

	private static Map<String, Field<?>> collectExtractors(List<CTCondition.ConceptConditions> expressions) {
		Map<String, Field<?>> extractors = new LinkedHashMap<>();
		for (CTCondition.ConceptConditions expression : expressions) {
			for (Map.Entry<Field<?>, CTCondition.FieldCondition> entry : expression.conditions().entrySet()) {
				extractors.putIfAbsent(entry.getKey().getName(), entry.getValue().extractor());
			}
		}
		return extractors;
	}

	private static List<RowN> expressionsToRows(
			TreeConcept concept,
			List<CTCondition.ConceptConditions> expressions,
			List<Field<?>> keyFields
	) {
		Map<List<Param<?>>, ConceptElement<?>> resolvedMappings = new HashMap<>();

		for (CTCondition.ConceptConditions expression : expressions) {
			Map<String, CTCondition.FieldCondition> conditions = expression.conditions().entrySet().stream()
					.collect(Collectors.toMap(entry -> entry.getKey().getName(), Map.Entry::getValue));
			List<Set<Param<?>>> valuesByField = new ArrayList<>();
			for (Field<?> keyField : keyFields) {
				CTCondition.FieldCondition fieldCondition = conditions.get(keyField.getName());
				valuesByField.add(fieldCondition != null ? fieldCondition.params() : Set.of(defaultValue(keyField)));
			}

			for (List<Param<?>> params : Sets.cartesianProduct(valuesByField)) {
				ConceptElement<?> previous = resolvedMappings.get(params);
				ConceptElement<?> current = expression.conceptElement();
				if (previous == null || previous.getDepth() < current.getDepth()) {
					resolvedMappings.put(params, current);
					continue;
				}
				if (previous.getDepth() == current.getDepth() && !previous.equals(current)) {
					throw new IllegalArgumentException(
							"Concept %s has overlapping sibling mappings for %s and %s on values %s"
									.formatted(concept.getId(), previous.getId(), current.getId(), params)
					);
				}
			}
		}

		return resolvedMappings.entrySet().stream()
				.map(entry -> {
					List<Param<?>> values = new ArrayList<>(entry.getKey().size() + 1);
					values.add(val(entry.getValue().getLocalId()));
					values.addAll(entry.getKey());
					return row(values);
				})
				.toList();
	}

	private static Param<?> defaultValue(Field<?> field) {
		if (field.getDataType().isBoolean()) {
			return inline(false);
		}
		if (field.getDataType().isString()) {
			return inline(null, String.class);
		}
		throw new IllegalStateException("Fields of type %s are not expected".formatted(field.getDataType()));
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
		return DSL.table(tableName);
	}

	public Field<Integer> resolvedId() {
		return field(name(tableName, name(RESOLVED_ID_COLUMN)), Integer.class);
	}

	public List<Field<?>> tableFields() {
		List<Field<?>> fields = new ArrayList<>(keyFields.size() + 1);
		fields.add(field(name(RESOLVED_ID_COLUMN), Integer.class));
		fields.addAll(keyFields);
		return fields;
	}

	public Condition joinCondition(Connector connector) {
		CTConditionContext context = CTConditionContext.forConnector(connector, functionProvider);
		List<CTCondition.ConceptConditions> expressions = collectAllExpressions(concept, null, context);
		Map<String, Field<?>> extractors = collectExtractors(expressions);

		Condition condition = noCondition();
		for (Field<?> keyField : keyFields) {
			Field<?> extractor = extractors.get(keyField.getName());
			if (extractor == null) {
				throw new IllegalArgumentException(
						"No connector expression for concept mapping field `%s` in connector %s"
								.formatted(keyField.getName(), connector.getId())
				);
			}
			Field<?> mappingField = field(name(tableName, keyField.getUnqualifiedName()), keyField.getDataType());
			condition = condition.and(mappingField.eq((Field) extractor));
		}

		return condition.equals(noCondition()) ? functionProvider.unconditionalJoinCondition() : condition;
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

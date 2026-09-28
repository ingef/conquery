package com.bakdata.conquery.sql.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Param;
import org.jooq.RowN;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/** Resolves concept condition expressions into the contents of a physical concept ID mapping table. */
public final class ConceptIdMappingResolver {

	private ConceptIdMappingResolver() {
	}

	public static ConceptIdMappingTable resolve(
			String conceptId,
			Name tableName,
			List<MappingExpression> expressions
	) {
		List<Field<?>> keyFields = collectKeyFields(expressions);
		return new ConceptIdMappingTable(tableName, keyFields, expressionsToRows(conceptId, expressions, keyFields));
	}

	public static Map<String, Field<?>> collectExtractors(List<MappingExpression> expressions) {
		Map<String, Field<?>> extractors = new LinkedHashMap<>();
		for (MappingExpression expression : expressions) {
			for (Map.Entry<Field<?>, FieldCondition> entry : expression.conditions().entrySet()) {
				extractors.putIfAbsent(entry.getKey().getName(), entry.getValue().extractor());
			}
		}
		return Map.copyOf(extractors);
	}

	private static List<Field<?>> collectKeyFields(List<MappingExpression> expressions) {
		Map<String, Field<?>> fields = new TreeMap<>();
		for (MappingExpression expression : expressions) {
			for (Field<?> field : expression.conditions().keySet()) {
				fields.merge(field.getName(), field, ConceptIdMappingResolver::mergeFieldType);
			}
		}
		return List.copyOf(fields.values());
	}

	private static Field<?> mergeFieldType(Field<?> left, Field<?> right) {
		DataType<?> leftType = left.getDataType();
		DataType<?> rightType = right.getDataType();
		if (leftType.isString() && rightType.isString()) {
			int length = Math.max(leftType.length(), rightType.length());
			return length > 0
					? DSL.field(left.getUnqualifiedName(), SQLDataType.VARCHAR(length))
					: DSL.field(left.getUnqualifiedName(), SQLDataType.VARCHAR);
		}
		if (!leftType.getType().equals(rightType.getType())) {
			throw new IllegalArgumentException(
					"Concept mapping field `%s` is used with incompatible types %s and %s"
							.formatted(left.getName(), leftType, rightType)
			);
		}
		return left;
	}

	private static List<RowN> expressionsToRows(
			String conceptId,
			List<MappingExpression> expressions,
			List<Field<?>> keyFields
	) {
		Map<List<Param<?>>, MappingExpression> resolvedMappings = new HashMap<>();
		for (MappingExpression expression : expressions) {
			Map<String, FieldCondition> conditions = new HashMap<>();
			expression.conditions().forEach((field, condition) -> conditions.put(field.getName(), condition));
			List<List<Param<?>>> valuesByField = new ArrayList<>();
			for (Field<?> keyField : keyFields) {
				FieldCondition condition = conditions.get(keyField.getName());
				valuesByField.add(condition == null ? List.of(defaultValue(keyField)) : List.copyOf(condition.params()));
			}
			for (List<Param<?>> params : cartesianProduct(valuesByField)) {
				MappingExpression previous = resolvedMappings.get(params);
				if (previous == null || previous.depth() < expression.depth()) {
					resolvedMappings.put(params, expression);
					continue;
				}
				if (previous.depth() == expression.depth() && !previous.elementId().equals(expression.elementId())) {
					throw new IllegalArgumentException(
							"Concept %s has overlapping sibling mappings for %s and %s on values %s"
									.formatted(conceptId, previous.elementId(), expression.elementId(), params)
					);
				}
			}
		}
		return resolvedMappings.entrySet().stream().map(entry -> {
			List<Field<?>> values = new ArrayList<>(entry.getKey().size() + 1);
			values.add(DSL.val(entry.getValue().localId()));
			values.addAll(entry.getKey());
			return DSL.row(values);
		}).toList();
	}

	private static Param<?> defaultValue(Field<?> field) {
		if (field.getDataType().isBoolean()) {
			return DSL.inline(false);
		}
		if (field.getDataType().isString()) {
			return DSL.inline(null, String.class);
		}
		throw new IllegalStateException("Fields of type %s are not expected".formatted(field.getDataType()));
	}

	private static List<List<Param<?>>> cartesianProduct(List<List<Param<?>>> valuesByField) {
		List<List<Param<?>>> product = new ArrayList<>();
		product.add(List.of());
		for (List<Param<?>> values : valuesByField) {
			List<List<Param<?>>> next = new ArrayList<>();
			for (List<Param<?>> prefix : product) {
				for (Param<?> value : values) {
					List<Param<?>> combined = new ArrayList<>(prefix.size() + 1);
					combined.addAll(prefix);
					combined.add(value);
					next.add(List.copyOf(combined));
				}
			}
			product = next;
		}
		return product;
	}

	public record FieldCondition(Field<?> extractor, Set<Param<?>> params) {
		public FieldCondition {
			params = Set.copyOf(params);
		}
	}

	public record MappingExpression(
			String elementId,
			int localId,
			int depth,
			Map<Field<?>, FieldCondition> conditions
	) {
		public MappingExpression {
			conditions = Map.copyOf(conditions);
		}
	}
}

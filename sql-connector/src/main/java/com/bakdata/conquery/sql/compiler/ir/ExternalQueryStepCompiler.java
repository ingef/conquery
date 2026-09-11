package com.bakdata.conquery.sql.compiler.ir;

import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.condition.DateRestrictionCondition;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import com.bakdata.conquery.sql.model.node.ExternalEntity;
import com.bakdata.conquery.sql.model.node.ExternalNode;
import com.bakdata.conquery.sql.model.range.DateRange;
import org.jooq.Field;
import org.jooq.Name;

/** Compiles resolved external entity values into literal-backed query-step IR. */
public final class ExternalQueryStepCompiler {

	private static final String EXTERNAL_IDS_CTE_NAME = "external_ids";
	private static final String EXTERNAL_VALUES_CTE_NAME = "external_extra";

	private ExternalQueryStepCompiler() {
	}

	public static ExternalQuerySteps compile(
			ExternalNode externalNode,
			boolean negate,
			Optional<DateRange> dateRestriction,
			CompilerDialect compilerDialect
	) {
		QueryStep entities = createEntityStep(externalNode, negate, dateRestriction, compilerDialect);
		Optional<QueryStep> values = externalNode.valueColumns().isEmpty()
				? Optional.empty()
				: Optional.of(createValuesStep(externalNode, negate, compilerDialect));
		return new ExternalQuerySteps(entities, values);
	}

	private static QueryStep createEntityStep(
			ExternalNode externalNode,
			boolean negate,
			Optional<DateRange> dateRestriction,
			CompilerDialect compilerDialect
	) {
		List<QueryStep> rows = externalNode.entities().stream()
				.flatMap(entity -> entityRows(entity, compilerDialect).stream())
				.toList();
		QueryStep allEntities = QueryStep.createUnionAllStep(
				rows,
				EXTERNAL_IDS_CTE_NAME,
				List.of(),
				negate
		);

		if (dateRestriction.isEmpty()) {
			return allEntities;
		}
		ColumnDateRange validityDate = allEntities.getSelects().getValidityDate().orElseThrow();
		return QueryStep.builder()
				.predecessor(allEntities)
				.conditions(List.of(new DateRestrictionCondition(
						compilerDialect.dateRangeLiteral(dateRestriction.orElseThrow()),
						validityDate
				).condition()))
				.selects(allEntities.getQualifiedSelects())
				.fromTable(table(name(allEntities.getCteName())))
				.cteName(EXTERNAL_IDS_CTE_NAME + "_date_restriction")
				.build();
	}

	private static List<QueryStep> entityRows(ExternalEntity entity, CompilerDialect compilerDialect) {
		SqlIdColumns ids = new SqlIdColumns(
				compilerDialect.externalId(entity.entityId()).as(SharedAliases.PRIMARY_COLUMN.getAlias())
		);
		List<ColumnDateRange> validityDates = entity.validityDates().isEmpty()
				? List.of(compilerDialect.emptyDateRange())
				: entity.validityDates().stream().map(compilerDialect::dateRangeLiteral).toList();

		return validityDates.stream()
				.map(validityDate -> literalRow(
						Selects.builder()
								.ids(ids)
								.validityDate(Optional.of(validityDate.as(SharedAliases.DATES_COLUMN.getAlias())))
								.build(),
						compilerDialect
				))
				.toList();
	}

	private static QueryStep createValuesStep(
			ExternalNode externalNode,
			boolean negate,
			CompilerDialect compilerDialect
	) {
		List<QueryStep> rows = externalNode.entities().stream()
				.map(entity -> valueRow(entity, externalNode.valueColumns(), compilerDialect))
				.toList();
		return QueryStep.createUnionAllStep(rows, EXTERNAL_VALUES_CTE_NAME, List.of(), negate);
	}

	private static QueryStep valueRow(
			ExternalEntity entity,
			List<String> valueColumns,
			CompilerDialect compilerDialect
	) {
		SqlIdColumns ids = new SqlIdColumns(
				compilerDialect.externalId(entity.entityId()).as(SharedAliases.PRIMARY_COLUMN.getAlias())
		);
		List<SqlSelect> values = new ArrayList<>(valueColumns.size());
		for (String valueColumn : valueColumns) {
			Field<?> expression = compilerDialect.externalStringValues(
					entity.values().getOrDefault(valueColumn, List.of())
			);
			Name alias = name(valueColumn.replace(' ', '_'));
			values.add(new FieldWrapper<>(expression.as(alias)));
		}
		return literalRow(Selects.builder().ids(ids).sqlSelects(values).build(), compilerDialect);
	}

	private static QueryStep literalRow(Selects selects, CompilerDialect compilerDialect) {
		return QueryStep.builder()
				.selects(selects)
				.fromTable(compilerDialect.literalSelectTable())
				.build();
	}
}

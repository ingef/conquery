package com.bakdata.conquery.sql.translate.query;

import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.model.node.ExternalEntity;
import com.bakdata.conquery.sql.model.node.ExternalNode;
import com.bakdata.conquery.sql.model.range.DateRange;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.Selects;
import com.bakdata.conquery.sql.translate.ir.SharedAliases;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.condition.DateRestrictionCondition;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.translate.ir.select.SqlSelect;
import lombok.experimental.UtilityClass;
import org.jooq.Field;
import org.jooq.Name;

/** Compiles resolved external entity values into literal-backed query-step IR. */
@UtilityClass
public final class ExternalQueryStepTranslator {

	private static final String EXTERNAL_IDS_CTE_NAME = "external_ids";
	private static final String EXTERNAL_VALUES_CTE_NAME = "external_extra";

	public static ExternalQuerySteps translate(
			ExternalNode externalNode,
			boolean negate,
			Optional<DateRange> dateRestriction,
			TranslatorDialect translatorDialect
	) {
		QueryStep entities = createEntityStep(externalNode, negate, dateRestriction, translatorDialect);
		Optional<QueryStep> values = externalNode.valueColumns().isEmpty()
				? Optional.empty()
				: Optional.of(createValuesStep(externalNode, negate, translatorDialect));
		return new ExternalQuerySteps(entities, values);
	}

	private static QueryStep createEntityStep(
			ExternalNode externalNode,
			boolean negate,
			Optional<DateRange> dateRestriction,
			TranslatorDialect translatorDialect
	) {
		List<QueryStep> rows = externalNode.entities().stream()
				.flatMap(entity -> entityRows(entity, translatorDialect).stream())
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
						translatorDialect.dateRangeLiteral(dateRestriction.orElseThrow()),
						validityDate
				).condition()))
				.selects(allEntities.getQualifiedSelects())
				.fromTable(table(name(allEntities.getCteName())))
				.cteName(EXTERNAL_IDS_CTE_NAME + "_date_restriction")
				.build();
	}

	private static List<QueryStep> entityRows(ExternalEntity entity, TranslatorDialect translatorDialect) {
		SqlIdColumns ids = new SqlIdColumns(
				translatorDialect.externalId(entity.entityId()).as(SharedAliases.PRIMARY_COLUMN.getAlias())
		);
		List<ColumnDateRange> validityDates = entity.validityDates().isEmpty()
				? List.of(translatorDialect.emptyDateRange())
				: entity.validityDates().stream().map(translatorDialect::dateRangeLiteral).toList();

		return validityDates.stream()
				.map(validityDate -> literalRow(
						Selects.builder()
								.ids(ids)
								.validityDate(Optional.of(validityDate.as(SharedAliases.DATES_COLUMN.getAlias())))
								.build(),
						translatorDialect
				))
				.toList();
	}

	private static QueryStep createValuesStep(
			ExternalNode externalNode,
			boolean negate,
			TranslatorDialect translatorDialect
	) {
		List<QueryStep> rows = externalNode.entities().stream()
				.map(entity -> valueRow(entity, externalNode.valueColumns(), translatorDialect))
				.toList();
		return QueryStep.createUnionAllStep(rows, EXTERNAL_VALUES_CTE_NAME, List.of(), negate);
	}

	private static QueryStep valueRow(
			ExternalEntity entity,
			List<String> valueColumns,
			TranslatorDialect translatorDialect
	) {
		SqlIdColumns ids = new SqlIdColumns(
				translatorDialect.externalId(entity.entityId()).as(SharedAliases.PRIMARY_COLUMN.getAlias())
		);
		List<SqlSelect> values = new ArrayList<>(valueColumns.size());
		for (String valueColumn : valueColumns) {
			Field<?> expression = translatorDialect.externalStringValues(
					entity.values().getOrDefault(valueColumn, List.of())
			);
			Name alias = name(valueColumn.replace(' ', '_'));
			values.add(new FieldWrapper<>(expression.as(alias)));
		}
		return literalRow(Selects.builder().ids(ids).sqlSelects(values).build(), translatorDialect);
	}

	private static QueryStep literalRow(Selects selects, TranslatorDialect translatorDialect) {
		return QueryStep.builder()
				.selects(selects)
				.fromTable(translatorDialect.literalSelectTable())
				.build();
	}
}

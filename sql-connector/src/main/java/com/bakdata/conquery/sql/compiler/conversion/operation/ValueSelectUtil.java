package com.bakdata.conquery.sql.compiler.conversion.operation;

import static org.jooq.impl.DSL.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.bakdata.conquery.sql.model.range.SubstringRange;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;

import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.select.SingleColumnSqlSelect;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.CteStep;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.SortField;

class ValueSelectUtil {

	public static ConnectorSqlSelects createValueSelect(
			ResolvedColumn column,
			String alias,
			Function<Field<?>, ? extends SortField<?>> ordering,
			Optional<SubstringRange> substringRange, SelectConversionContext selectContext) {


		SingleColumnSqlSelect rootSelect = SubstringSelect.getSubstringSelect(column, substringRange, selectContext.tables().getRootTable(), alias);

		// create a CTE, that per row makes a window calculation to select for the rank of the validity date.
		// Further down below, we select the values with rank=1, which is FIRST/LAST depending on sort order supplied by the creator.

		QueryStep rowNumberStep =
				buildRowNumberStep(rootSelect, ordering, alias, selectContext);

		QueryStep rowFilterStep = buildRowFilterStep(rowNumberStep, alias, selectContext.ids());

		SqlSelect finalSelect = rowFilterStep.getQualifiedSelects().getSqlSelects().getFirst();

		FieldWrapper<SqlSelect> aggregationSelect =
				new FieldWrapper<>(field(coalesce(finalSelect.qualify(ValueSelectCteStep.ROW_SELECT_STEP.cteName(alias)))).as(alias), column.physicalName());

		return ConnectorSqlSelects.builder()
								  .additionalPredecessor(Optional.of(rowFilterStep))
								  .preprocessingSelect(rootSelect)
								  .finalSelect(aggregationSelect)
								  .build();
	}

	private static QueryStep buildRowNumberStep(
			SingleColumnSqlSelect rootSelect, Function<Field<?>, ? extends SortField<?>> ordering, String alias,
			SelectConversionContext selectContext) {

		CompilerDialect functionProvider = selectContext.dialect();

		String predecessor = selectContext.tables().getPredecessor(ConceptCteStep.AGGREGATION_SELECT);
		Field<?> qualifiedRootSelect = rootSelect.qualify(predecessor).select();

		return QueryStep.builder()
						.selects(Selects.builder()
										.ids(selectContext.ids().qualify(null))
										.sqlSelects(List.of(
												new FieldWrapper<>(qualifiedRootSelect.as(alias), qualifiedRootSelect.getName()),
												rowNumberField(predecessor, selectContext.validityDate(), ordering,
															   selectContext.ids(),
															   functionProvider
												)
										))
										.build())
						.cteName(ValueSelectCteStep.ROW_NUMBER_STEP.cteName(alias))
						.conditions(List.of(qualifiedRootSelect.isNotNull()))
						.fromTable(table(name(predecessor)))
						.build();
	}

	private static QueryStep buildRowFilterStep(QueryStep rowNumberStep, String alias, SqlIdColumns idColumns) {
		Field<Object> rowNumber = field(name("row-number"));

		SqlIdColumns unqualifiedIds = idColumns.qualify(null);
		SelectConditionStep<Record> selectFirst =
				select(unqualifiedIds.toFields())
						.select(field(name(alias)), field(name("select-result", "row-number")))
						.from(table(name(ValueSelectCteStep.ROW_NUMBER_STEP.cteName(alias)))
									  .as("select-result"))
						.where(or(rowNumber.equal(inline(1)), rowNumber.isNull()));


		return QueryStep.builder()
						.predecessor(rowNumberStep)
						.selects(Selects.builder()
										.ids(unqualifiedIds)
										.sqlSelect(new FieldWrapper<>(selectFirst.field(alias)))
										.build())
						.cteName(ValueSelectCteStep.ROW_SELECT_STEP.cteName(alias))
						.fromTable(selectFirst)
						.build();
	}


	private static FieldWrapper<Integer> rowNumberField(
			String predecessor, Optional<ColumnDateRange> validityDate, Function<Field<?>, ? extends SortField<?>> ordering,
			SqlIdColumns idColumns, CompilerDialect functionProvider) {

		List<Field<?>> qualifiedIds = idColumns.qualify(predecessor).toFields();

		if (validityDate.isEmpty()) {
			return new FieldWrapper<>(
					rowNumber().over(partitionBy(qualifiedIds).orderBy(qualifiedIds)).as("row-number"),
					new String[0]
			);
		}

		return new FieldWrapper<>(
				rowNumber().over(
						partitionBy(qualifiedIds)
								.orderBy(functionProvider.orderByValidityDates(ordering, validityDate.get().qualify(predecessor).toFields()))
				).as("row-number"),
				new String[0]
		);
	}


	@RequiredArgsConstructor
	@Getter
	enum ValueSelectCteStep implements CteStep {
		ROW_NUMBER_STEP("value_select_assign_row_number_step", ConceptCteStep.PREPROCESSING), ROW_SELECT_STEP("value_select_first_row_step", ROW_NUMBER_STEP);

		private final String suffix;
		private final CteStep predecessor;
	}

}

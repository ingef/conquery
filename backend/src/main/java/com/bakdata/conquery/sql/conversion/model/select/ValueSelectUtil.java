package com.bakdata.conquery.sql.conversion.model.select;

import static org.jooq.impl.DSL.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.SortField;
import org.jooq.Table;

class ValueSelectUtil {

	public static ConnectorSqlSelects createValueSelect(
			Column column,
			String alias,
			Function<Field<?>, ? extends SortField<?>> ordering,
			SelectContext<ConnectorSqlTables> selectContext) {


		ExtractingSqlSelect<?> rootSelect = new ExtractingSqlSelect<>(selectContext.getTables().getRootTable(), column.getName(), Object.class);

		// create a CTE, that per row makes a window calculation to select for the rank of the validity date.
		// Further down below, we select the values with rank=1, which is FIRST/LAST depending on sort order supplied by the creator.

		QueryStep rowNumberStep =
				buildRowNumberStep(rootSelect, ordering, alias, selectContext);

		QueryStep rowFilterStep = buildRowFilterStep(rowNumberStep, alias, selectContext);

		SqlSelect finalSelect = rowFilterStep.getQualifiedSelects().getSqlSelects().getFirst();

		FieldWrapper<SqlSelect> aggregationSelect =
				new FieldWrapper<>(field(coalesce(finalSelect.qualify(ValueSelectCteStep.ROW_SELECT_STEP.cteName(alias)))).as(alias), column.getName());

		//TODO to get stratification working, i need to do the row-numbering as an aggregation select.

		return ConnectorSqlSelects.builder()
								  .additionalPredecessor(Optional.of(rowFilterStep))
								  .preprocessingSelect(rootSelect)
								  .finalSelect(aggregationSelect)
								  .build();
	}

	private static QueryStep buildRowNumberStep(
			ExtractingSqlSelect<?> rootSelect, Function<Field<?>, ? extends SortField<?>> ordering, String alias,
			SelectContext<ConnectorSqlTables> selectContext) {

		String predecessor = selectContext.getTables().getPredecessor(ConceptCteStep.AGGREGATION_SELECT);


		Field<?> qualifiedRootSelect = rootSelect.qualify(predecessor).select();
		List<Field<?>> ids = selectContext.getIds().qualify(predecessor).toFields();
		Table<Record> predecessorTable = table(name(predecessor));

		List<Field<?>> validityDateFields =
				selectContext.getValidityDate().map(dateRange -> dateRange.qualify(predecessor))
							 .map(ColumnDateRange::toFields)
							 .orElse(Collections.emptyList());

		return QueryStep.builder()
						.selects(Selects.builder()
										.ids(selectContext.getIds().qualify(null))
										.sqlSelects(List.of(
												new FieldWrapper<>(qualifiedRootSelect.as(alias), qualifiedRootSelect.getName()),
												new FieldWrapper<>(rowNumber().over(partitionBy(ids)
																							.orderBy(selectContext.getFunctionProvider().getOrdering(ordering,
																																					 validityDateFields

																									 ).orElse(ids)
																							)

												).as("row-number"),
																   new String[0] // If I don't supply it, "row-number" is requested for event-filter CTE

												)
										))
										.build())
						.cteName(ValueSelectCteStep.ROW_NUMBER_STEP.cteName(alias))
						.conditions(List.of(qualifiedRootSelect.isNotNull()))
						.fromTable(predecessorTable)
						.build();
	}

	private static QueryStep buildRowFilterStep(QueryStep rowNumberStep, String alias, SelectContext<ConnectorSqlTables> selectContext) {
		Field<Object> rowNumber = field(name("row-number"));

		SelectConditionStep<Record> coalesced =
				select(selectContext.getIds().qualify(null).toFields())
						.select(field(name(alias)), field(name("select-result", "row-number")))
						.from(table(name(ValueSelectCteStep.ROW_NUMBER_STEP.cteName(alias)))
									  .as("select-result"))
						.where(or(rowNumber.equal(val(1)), rowNumber.isNull()));

		return QueryStep.builder()
						.predecessor(rowNumberStep)
						.selects(Selects.builder()
										.ids(selectContext.getIds().qualify(null))
										.sqlSelects(List.of(new FieldWrapper<>(coalesced.field(alias))))
										.build())
						.cteName(ValueSelectCteStep.ROW_SELECT_STEP.cteName(alias))
						.fromTable(coalesced)
						.build();
	}


	@RequiredArgsConstructor
	@Getter
	enum ValueSelectCteStep implements CteStep {
		ROW_NUMBER_STEP("value_select_assign_row_number_step", ConceptCteStep.EVENT_FILTER), ROW_SELECT_STEP("value_select_first_row_step", ROW_NUMBER_STEP);

		private final String suffix;
		private final CteStep predecessor;
	}

}

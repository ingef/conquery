package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.dialect.IntervalPacker;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QualifyingUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
public class AnsiSqlIntervalPacker implements IntervalPacker {

	public QueryStep createIntervalPackingSteps(IntervalPackingContext context) {
		QueryStep previousEndStep = createPreviousEndStep(context);
		QueryStep rangeIndexStep = createRangeIndexStep(previousEndStep, context);
		QueryStep intervalCompleteStep = createIntervalCompleteStep(rangeIndexStep, context);
		return intervalCompleteStep;
	}

	private QueryStep createPreviousEndStep(IntervalPackingContext context) {

		String sourceTableName = context.getIntervalPackingTables().getRootTable();
		Field<Object> primaryColumn = QualifyingUtil.qualify(context.getPrimaryColumn(), sourceTableName);
		ColumnDateRange validityDate = context.getValidityDate().qualify(sourceTableName);

		Field<Date> previousEnd = DSL.max(validityDate.getEnd())
									 .over(DSL.partitionBy(primaryColumn)
											  .orderBy(validityDate.getStart(), validityDate.getEnd())
											  .rowsBetweenUnboundedPreceding()
											  .andPreceding(1))
									 .as(IntervalPacker.PREVIOUS_END_FIELD_NAME);

		ArrayList<SqlSelect> qualifiedSelects = new ArrayList<>(QualifyingUtil.qualify(context.getCarryThroughSelects(), sourceTableName));
		qualifiedSelects.add(new FieldWrapper(previousEnd));

		Selects previousEndSelects = Selects.builder()
											.primaryColumn(primaryColumn)
											.validityDate(Optional.of(validityDate))
											.sqlSelects(qualifiedSelects)
											.build();

		return QueryStep.builder()
						.cteName(context.getIntervalPackingTables().cteName(IntervalPackingCteStep.PREVIOUS_END))
						.selects(previousEndSelects)
						.fromTable(QueryStep.toTableLike(sourceTableName))
						.predecessors(context.getPredecessor() == null ? Collections.emptyList() : List.of(context.getPredecessor()))
						.build();
	}

	private QueryStep createRangeIndexStep(QueryStep previousEndStep, IntervalPackingContext context) {

		String previousEndCteName = previousEndStep.getCteName();
		Selects previousEndSelects = previousEndStep.getQualifiedSelects();
		Field<Object> primaryColumn = previousEndSelects.getPrimaryColumn();
		ColumnDateRange validityDate = previousEndSelects.getValidityDate().get();
		Field<Date> previousEnd = DSL.field(DSL.name(previousEndCteName, IntervalPacker.PREVIOUS_END_FIELD_NAME), Date.class);

		Field<BigDecimal> rangeIndex =
				DSL.sum(
						   DSL.when(validityDate.getStart().greaterThan(previousEnd), DSL.val(1))
							  .otherwise(DSL.inline(null, Integer.class)))
				   .over(DSL.partitionBy(primaryColumn)
							.orderBy(validityDate.getStart(), validityDate.getEnd())
							.rowsUnboundedPreceding())
				   .as(IntervalPacker.RANGE_INDEX_FIELD_NAME);

		ArrayList<SqlSelect> qualifiedSelects = new ArrayList<>(QualifyingUtil.qualify(context.getCarryThroughSelects(), previousEndCteName));
		qualifiedSelects.add(new FieldWrapper(rangeIndex));

		Selects rangeIndexSelects = Selects.builder()
										   .primaryColumn(primaryColumn)
										   .validityDate(Optional.of(validityDate))
										   .sqlSelects(qualifiedSelects)
										   .build();

		return QueryStep.builder()
						.cteName(context.getIntervalPackingTables().cteName(IntervalPackingCteStep.RANGE_INDEX))
						.selects(rangeIndexSelects)
						.fromTable(QueryStep.toTableLike(previousEndCteName))
						.predecessors(List.of(previousEndStep))
						.build();
	}

	private QueryStep createIntervalCompleteStep(QueryStep rangeIndexStep, IntervalPackingContext context) {

		String rangeIndexCteName = rangeIndexStep.getCteName();
		Selects rangeIndexSelects = rangeIndexStep.getQualifiedSelects();
		Field<Object> primaryColumn = rangeIndexSelects.getPrimaryColumn();
		ColumnDateRange validityDate = rangeIndexSelects.getValidityDate().get();

		Field<Date> rangeStart = DSL.min(validityDate.getStart()).as(IntervalPacker.RANGE_START_MIN_FIELD_NAME);
		Field<Date> rangeEnd = DSL.max(validityDate.getEnd()).as(IntervalPacker.RANGE_END_MAX_FIELD_NAME);
		Field<BigDecimal> rangeIndex = DSL.field(DSL.name(rangeIndexCteName, IntervalPacker.RANGE_INDEX_FIELD_NAME), BigDecimal.class);

		List<SqlSelect> qualifiedSelects = QualifyingUtil.qualify(context.getCarryThroughSelects(), rangeIndexCteName);
		Selects intervalCompleteSelects = Selects.builder()
												 .primaryColumn(primaryColumn)
												 .validityDate(Optional.of(ColumnDateRange.of(rangeStart, rangeEnd)))
												 .sqlSelects(qualifiedSelects)
												 .build();

		// we group range start and end by range index
		List<Field<?>> groupBySelects = new ArrayList<>();
		groupBySelects.add(primaryColumn);
		groupBySelects.add(rangeIndex);
		qualifiedSelects.stream().map(SqlSelect::select).forEach(groupBySelects::add);

		return QueryStep.builder()
						.cteName(context.getIntervalPackingTables().cteName(IntervalPackingCteStep.INTERVAL_COMPLETE))
						.selects(intervalCompleteSelects)
						.fromTable(QueryStep.toTableLike(rangeIndexCteName))
						.predecessors(List.of(rangeIndexStep))
						.groupBy(groupBySelects)
						.build();
	}

}

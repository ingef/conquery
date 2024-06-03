package com.bakdata.conquery.sql.conversion.cqelement.intervalpacking;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.dialect.IntervalPacker;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QualifyingUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
public class AnsiSqlIntervalPacker implements IntervalPacker {

	@Override
	public QueryStep aggregateAsValidityDate(IntervalPackingContext context) {
		return aggregateDate(context, AggregationMode.VALIDITY_DATE);
	}

	@Override
	public QueryStep aggregateAsArbitrarySelect(IntervalPackingContext context) {
		return aggregateDate(context, AggregationMode.ARBITRARY_SELECT);
	}

	private QueryStep aggregateDate(IntervalPackingContext context, AggregationMode aggregationMode) {
		QueryStep previousEndStep = createPreviousEndStep(context, aggregationMode);
		QueryStep rangeIndexStep = createRangeIndexStep(previousEndStep, context, aggregationMode);
		QueryStep intervalCompleteStep = createIntervalCompleteStep(rangeIndexStep, context, aggregationMode);
		return intervalCompleteStep;
	}

	private QueryStep createPreviousEndStep(IntervalPackingContext context, AggregationMode aggregationMode) {

		String sourceTableName = context.getTables().getPredecessor(IntervalPackingCteStep.PREVIOUS_END);
		SqlIdColumns ids = context.getIds().qualify(sourceTableName);
		ColumnDateRange daterange = context.getDaterange().qualify(sourceTableName);

		Field<Date> previousEnd = DSL.max(daterange.getEnd())
									 .over(DSL.partitionBy(ids.toFields())
											  .orderBy(daterange.getStart(), daterange.getEnd())
											  .rowsBetweenUnboundedPreceding()
											  .andPreceding(1))
									 .as(IntervalPacker.PREVIOUS_END_FIELD_NAME);

		List<SqlSelect> qualifiedSelects = new ArrayList<>(QualifyingUtil.qualify(context.getCarryThroughSelects(), sourceTableName));
		qualifiedSelects.add(new FieldWrapper<>(previousEnd, daterange.getStart().getName(), daterange.getEnd().getName()));

		Selects previousEndSelects = buildSelects(ids, daterange, qualifiedSelects, aggregationMode);

		return QueryStep.builder()
						.cteName(context.getTables().cteName(IntervalPackingCteStep.PREVIOUS_END))
						.selects(previousEndSelects)
						.fromTable(QueryStep.toTableLike(sourceTableName))
						.predecessors(Optional.ofNullable(context.getPredecessor()).stream().toList())
						.build();
	}

	private QueryStep createRangeIndexStep(QueryStep previousEndStep, IntervalPackingContext context, AggregationMode aggregationMode) {

		String previousEndCteName = previousEndStep.getCteName();
		Selects previousEndSelects = previousEndStep.getQualifiedSelects();
		SqlIdColumns ids = previousEndSelects.getIds();
		ColumnDateRange daterange = context.getDaterange().qualify(previousEndCteName);
		Field<Date> previousEnd = DSL.field(DSL.name(previousEndCteName, IntervalPacker.PREVIOUS_END_FIELD_NAME), Date.class);

		Field<BigDecimal> rangeIndex =
				DSL.sum(
						   DSL.when(daterange.getStart().greaterThan(previousEnd), DSL.val(1))
							  .otherwise(DSL.inline(null, Integer.class)))
				   .over(DSL.partitionBy(ids.toFields())
							.orderBy(daterange.getStart(), daterange.getEnd())
							.rowsUnboundedPreceding())
				   .as(IntervalPacker.RANGE_INDEX_FIELD_NAME);

		List<SqlSelect> qualifiedSelects = new ArrayList<>(QualifyingUtil.qualify(context.getCarryThroughSelects(), previousEndCteName));
		qualifiedSelects.add(new FieldWrapper<>(rangeIndex));

		Selects rangeIndexSelects = buildSelects(ids, daterange, qualifiedSelects, aggregationMode);

		return QueryStep.builder()
						.cteName(context.getTables().cteName(IntervalPackingCteStep.RANGE_INDEX))
						.selects(rangeIndexSelects)
						.fromTable(QueryStep.toTableLike(previousEndCteName))
						.predecessors(List.of(previousEndStep))
						.build();
	}

	private QueryStep createIntervalCompleteStep(QueryStep rangeIndexStep, IntervalPackingContext context, AggregationMode aggregationMode) {

		String rangeIndexCteName = rangeIndexStep.getCteName();
		Selects rangeIndexSelects = rangeIndexStep.getQualifiedSelects();
		SqlIdColumns ids = rangeIndexSelects.getIds();
		ColumnDateRange daterange = context.getDaterange().qualify(rangeIndexCteName);

		Field<Date> rangeStart = DSL.min(daterange.getStart()).as(daterange.getStart().getName());
		Field<Date> rangeEnd = DSL.max(daterange.getEnd()).as(daterange.getEnd().getName());
		ColumnDateRange minMax = ColumnDateRange.of(rangeStart, rangeEnd);
		Field<BigDecimal> rangeIndex = DSL.field(DSL.name(rangeIndexCteName, IntervalPacker.RANGE_INDEX_FIELD_NAME), BigDecimal.class);

		List<SqlSelect> qualifiedSelects = QualifyingUtil.qualify(context.getCarryThroughSelects(), rangeIndexCteName);
		Selects intervalCompleteSelects = buildSelects(ids, minMax, qualifiedSelects, aggregationMode);

		// we group range start and end by range index
		List<Field<?>> groupBySelects = new ArrayList<>();
		groupBySelects.addAll(ids.toFields());
		groupBySelects.add(rangeIndex);
		qualifiedSelects.stream().flatMap(sqlSelect -> sqlSelect.toFields().stream()).forEach(groupBySelects::add);

		return QueryStep.builder()
						.cteName(context.getTables().cteName(IntervalPackingCteStep.INTERVAL_COMPLETE))
						.selects(intervalCompleteSelects)
						.fromTable(QueryStep.toTableLike(rangeIndexCteName))
						.predecessors(List.of(rangeIndexStep))
						.groupBy(groupBySelects)
						.build();
	}

	private static Selects buildSelects(
			SqlIdColumns ids,
			ColumnDateRange daterange,
			List<SqlSelect> carryThroughSelects,
			AggregationMode aggregationMode
	) {
		Selects.SelectsBuilder builder = Selects.builder()
												.ids(ids);
		switch (aggregationMode) {
			case VALIDITY_DATE -> builder.validityDate(Optional.of(daterange));
			case ARBITRARY_SELECT -> builder.sqlSelect(daterange);
		}

		return builder.sqlSelects(carryThroughSelects).build();
	}

}

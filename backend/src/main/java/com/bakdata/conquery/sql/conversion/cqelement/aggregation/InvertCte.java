package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.sql.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QualifyingUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SelectsIds;
import lombok.Getter;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.TableLike;
import org.jooq.TableOnConditionStep;
import org.jooq.impl.DSL;

/**
 * Concept for date range inversion taken from <a href="https://explainextended.com/2009/11/09/inverting-date-ranges/">Inverting date ranges</a>.
 */
@Getter
class InvertCte extends DateAggregationCte {

	public static final String ROWS_LEFT_TABLE_NAME = "rows_left";
	public static final String ROWS_RIGHT_TABLE_NAME = "rows_right";

	private final DateAggregationCteStep cteStep;

	public InvertCte(DateAggregationCteStep cteStep) {
		this.cteStep = cteStep;
	}

	@Override
	protected QueryStep.QueryStepBuilder convertStep(DateAggregationContext context) {

		QueryStep rowNumberStep = context.getStep(InvertCteStep.ROW_NUMBER);

		SelectsIds ids = context.getIds();
		SelectsIds leftIds = ids.qualify(ROWS_LEFT_TABLE_NAME);
		SelectsIds rightIds = ids.qualify(ROWS_RIGHT_TABLE_NAME);
		SelectsIds coalescedIds = SelectsIds.coalesce(List.of(leftIds, rightIds));

		Selects invertSelects = getInvertSelects(rowNumberStep, coalescedIds, context);
		TableOnConditionStep<Record> fromTable = selfJoinWithShiftedRows(leftIds, rightIds, rowNumberStep);

		return QueryStep.builder()
						.selects(invertSelects)
						.fromTable(fromTable);
	}

	private Selects getInvertSelects(QueryStep rowNumberStep, SelectsIds coalescedIds, DateAggregationContext context) {

		SqlFunctionProvider functionProvider = context.getFunctionProvider();
		ColumnDateRange validityDate = rowNumberStep.getSelects().getValidityDate().get();

		Field<Date> rangeStart = DSL.coalesce(
				QualifyingUtil.qualify(validityDate.getEnd(), ROWS_LEFT_TABLE_NAME),
				functionProvider.toDateField(functionProvider.getMinDateExpression())
		).as(DateAggregationCte.RANGE_START);

		Field<Date> rangeEnd = DSL.coalesce(
				QualifyingUtil.qualify(validityDate.getStart(), ROWS_RIGHT_TABLE_NAME),
				functionProvider.toDateField(functionProvider.getMaxDateExpression())
		).as(DateAggregationCte.RANGE_END);

		return Selects.builder()
					  .ids(coalescedIds)
					  .validityDate(Optional.of(ColumnDateRange.of(rangeStart, rangeEnd)))
					  .sqlSelects(context.getCarryThroughSelects())
					  .build();
	}

	private TableOnConditionStep<Record> selfJoinWithShiftedRows(SelectsIds leftPrimaryColumn, SelectsIds rightPrimaryColumn, QueryStep rowNumberStep) {

		Field<Integer> leftRowNumber = DSL.field(DSL.name(ROWS_LEFT_TABLE_NAME, RowNumberCte.ROW_NUMBER_FIELD_NAME), Integer.class)
										  .plus(1);
		Field<Integer> rightRowNumber = DSL.field(DSL.name(ROWS_RIGHT_TABLE_NAME, RowNumberCte.ROW_NUMBER_FIELD_NAME), Integer.class);

		Condition[] joinConditions = Stream.concat(
												   Stream.of(leftRowNumber.eq(rightRowNumber)),
												   SelectsIds.join(leftPrimaryColumn, rightPrimaryColumn).stream()
										   )
										   .toArray(Condition[]::new);

		TableLike<Record> rowNumberTable = QueryStep.toTableLike(rowNumberStep.getCteName());
		return rowNumberTable.asTable(ROWS_LEFT_TABLE_NAME)
							 .fullJoin(rowNumberTable.asTable(ROWS_RIGHT_TABLE_NAME))
							 .on(joinConditions);
	}

}

package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.sql.Date;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QualifyingUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
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
	public static final String PRIMARY_COLUMN_FIELD_NAME = "primary_column";

	private final DateAggregationStep cteStep;

	public InvertCte(DateAggregationStep cteStep) {
		this.cteStep = cteStep;
	}

	@Override
	protected QueryStep.QueryStepBuilder convertStep(DateAggregationContext context) {

		QueryStep rowNumberStep = context.getStep(InvertStep.ROW_NUMBER);

		Field<Object> primaryColumn = context.getPrimaryColumn();
		Field<Object> leftPrimaryColumn = QualifyingUtil.qualify(primaryColumn, ROWS_LEFT_TABLE_NAME);
		Field<Object> rightPrimaryColumn = QualifyingUtil.qualify(primaryColumn, ROWS_RIGHT_TABLE_NAME);
		Field<Object> coalescedPrimaryColumn = DSL.coalesce(leftPrimaryColumn, rightPrimaryColumn)
												  .as(PRIMARY_COLUMN_FIELD_NAME);

		Selects invertSelects = getInvertSelects(rowNumberStep, coalescedPrimaryColumn, context);
		TableOnConditionStep<Record> fromTable = selfJoinWithShiftedRows(leftPrimaryColumn, rightPrimaryColumn, rowNumberStep);

		return QueryStep.builder()
						.selects(invertSelects)
						.fromTable(fromTable);
	}

	private Selects getInvertSelects(
			QueryStep rowNumberStep,
			Field<Object> coalescedPrimaryColumn,
			DateAggregationContext context
	) {

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
					  .primaryColumn(coalescedPrimaryColumn)
					  .validityDate(Optional.of(ColumnDateRange.of(rangeStart, rangeEnd)))
					  .explicitSelects(context.getCarryThroughSelects())
					  .build();
	}

	private TableOnConditionStep<Record> selfJoinWithShiftedRows(Field<Object> leftPrimaryColumn, Field<Object> rightPrimaryColumn, QueryStep rowNumberStep) {

		Field<Integer> leftRowNumber = DSL.field(DSL.name(ROWS_LEFT_TABLE_NAME, RowNumberCte.ROW_NUMBER_FIELD_NAME), Integer.class)
										  .plus(1);
		Field<Integer> rightRowNumber = DSL.field(DSL.name(ROWS_RIGHT_TABLE_NAME, RowNumberCte.ROW_NUMBER_FIELD_NAME), Integer.class);

		Condition joinCondition = leftPrimaryColumn.eq(rightPrimaryColumn)
												   .and(leftRowNumber.eq(rightRowNumber));

		TableLike<Record> rowNumberTable = QueryStep.toTableLike(rowNumberStep.getCteName());
		return rowNumberTable.asTable(ROWS_LEFT_TABLE_NAME)
							 .fullJoin(rowNumberTable.asTable(ROWS_RIGHT_TABLE_NAME))
							 .on(joinCondition);
	}


}

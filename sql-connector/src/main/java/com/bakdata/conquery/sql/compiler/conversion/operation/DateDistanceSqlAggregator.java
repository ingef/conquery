package com.bakdata.conquery.sql.compiler.conversion.operation;

import java.sql.Date;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;
import com.bakdata.conquery.sql.model.schema.ResolvedColumn;
import org.jooq.Field;
import org.jooq.impl.DSL;

public final class DateDistanceSqlAggregator {
	private DateDistanceSqlAggregator() {
	}

	static ConnectorSqlSelects connectorSelect(BuiltInSelects.DateDistance select, SelectConversionContext context) {
		Field<Date> startDate = DSL.field(DSL.name(context.tables().getRootTable(), select.column().physicalName()), Date.class);
		return selects(context.dialect().dateDistance(select.unit(), startDate, select.endDate()), context);
	}

	/** Supports a resolved per-row end date, as used by stratified forms. */
	public static ConnectorSqlSelects connectorSelect(ResolvedColumn column, ChronoUnit unit, Field<Date> endDate, SelectConversionContext context) {
		Field<Date> startDate = DSL.field(DSL.name(context.tables().getRootTable(), column.physicalName()), Date.class);
		return selects(context.dialect().dateDistance(unit, startDate, endDate), context);
	}

	private static ConnectorSqlSelects selects(Field<Integer> dateDistanceCalculation, SelectConversionContext context) {
		String alias = context.alias();
		FieldWrapper<Integer> dateDistanceSelect = new FieldWrapper<>(dateDistanceCalculation.as(alias));
		Field<Integer> qualifiedDateDistance = dateDistanceSelect.qualify(context.tables().getPredecessor(ConceptCteStep.AGGREGATION_SELECT)).select();
		FieldWrapper<Integer> minDateDistance = new FieldWrapper<>(DSL.min(qualifiedDateDistance).as(alias));
		return ConnectorSqlSelects.builder()
				.preprocessingSelect(dateDistanceSelect)
				.aggregationSelect(minDateDistance)
				.finalSelect(minDateDistance.qualify(context.tables().getPredecessor(ConceptCteStep.AGGREGATION_FILTER)))
				.build();
	}
}

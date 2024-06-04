package com.bakdata.conquery.sql.conversion.model.filter;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class DateDistanceCondition extends RangeCondition {

	public DateDistanceCondition(Field<Integer> column, Range.LongRange range) {
		super(column, range);
	}

	public static DateDistanceCondition onColumn(Column column, ChronoUnit timeUnit, FilterContext<Range.LongRange> filterContext) {

		String tableName = column.getTable().getName();
		String columnName = column.getName();
		Field<Date> startDateField = DSL.field(DSL.name(tableName, columnName), Date.class);

		ConversionContext conversionContext = filterContext.getConversionContext();
		SqlFunctionProvider functionProvider = filterContext.getSqlDialect().getFunctionProvider();
		LocalDate endDate = conversionContext.getSqlDialect().getDateNowSupplier().getLocalDateNow();
		Field<Date> endDateField = functionProvider.toDateField(Date.valueOf(endDate).toString());

		Field<Integer> dateDistance = functionProvider.dateDistance(timeUnit, startDateField, endDateField);
		return new DateDistanceCondition(dateDistance, filterContext.getValue());
	}

	@Override
	public ConditionType type() {
		return ConditionType.EVENT;
	}

}

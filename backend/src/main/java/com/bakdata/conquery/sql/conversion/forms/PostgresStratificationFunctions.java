package com.bakdata.conquery.sql.conversion.forms;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Map;

import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.sql.conversion.dialect.PostgreSqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.DatePart;
import org.jooq.Field;
import org.jooq.Keyword;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

@Getter
@RequiredArgsConstructor
class PostgresStratificationFunctions extends StratificationFunctions {

	private static final Map<Interval, Field<String>> INTERVAL_MAP = Map.of(
			Interval.ONE_YEAR_INTERVAL, DSL.val("1 year"),
			Interval.YEAR_AS_DAYS_INTERVAL, DSL.val("365 days"),
			Interval.THREE_MONTHS_INTERVAL, DSL.val("3 months"),
			Interval.NINETY_DAYS_INTERVAL, DSL.val("90 days"),
			Interval.ONE_DAY_INTERVAL, DSL.val("1 day")
	);

	private static final Keyword INTERVAL_KEYWORD = DSL.keyword("interval");

	private final PostgreSqlFunctionProvider functionProvider;

	@Override
	public Field<Date> absoluteIndexStartDate(ColumnDateRange dateRange) {
		return lower(dateRange);
	}

	@Override
	public Field<Date> yearStart(ColumnDateRange dateRange) {
		return dateTruncate(DSL.val("year"), dateRange);
	}

	@Override
	public Field<Date> quarterStart(ColumnDateRange dateRange) {
		Field<Integer> quarter = functionProvider.extract(DatePart.QUARTER, lower(dateRange));
		return DSL.field(
				"{0} + ({1} - 1) * {2} {3}",
				Date.class,
				yearStart(dateRange),
				quarter,
				INTERVAL_KEYWORD,
				INTERVAL_MAP.get(Interval.THREE_MONTHS_INTERVAL)
		);
	}

	@Override
	public Field<Integer> intSeriesField() {
		return StratificationFunctions.SERIES_INDEX;
	}

	@Override
	public Table<Record> generateIntSeries(int from, int to) {
		return DSL.table("generate_series({0}, {1})", from, to);
	}

	@Override
	protected Field<Date> lower(ColumnDateRange dateRange) {
		checkIsSingleColumnRange(dateRange);
		return DSL.function("lower", Date.class, dateRange.getRange());
	}

	@Override
	protected Field<Date> upper(ColumnDateRange dateRange) {
		checkIsSingleColumnRange(dateRange);
		return DSL.function("upper", Date.class, dateRange.getRange());
	}

	@Override
	protected Field<Integer> round(Field<? extends Number> number) {
		return (Field<Integer>) DSL.round(number);
	}

	@Override
	protected ColumnDateRange calcRange(Field<Date> start, Interval interval) {
		Field<String> intervalExpression = INTERVAL_MAP.get(interval);
		return ColumnDateRange.of(functionProvider.daterange(
				calcStartDate(start, intervalExpression),
				calcEndDate(start, intervalExpression),
				"[)"
		));
	}

	private Field<Date> calcStartDate(Field<Date> start, Field<String> intervalExpression) {
		return multiplyByInterval(start, intervalExpression, 1);
	}

	private Field<Date> calcEndDate(Field<Date> start, Field<String> intervalExpression) {
		return multiplyByInterval(start, intervalExpression, 0);
	}

	private Field<Date> multiplyByInterval(Field<Date> start, Field<String> intervalExpression, int offset) {
		Field<Timestamp> shiftedDate = DSL.field("{0} + {1} {2}", Timestamp.class, start, INTERVAL_KEYWORD, intervalExpression)
										  .times(intSeriesField().minus(offset));
		// cast to date because we only want the date from the timestamp
		return DSL.field("{0}::{1}", Date.class, shiftedDate, DSL.keyword("date"));
	}

	private Field<Date> dateTruncate(Field<String> field, ColumnDateRange dateRange) {
		return DSL.function("date_trunc", Date.class, field, lower(dateRange));
	}

	private static void checkIsSingleColumnRange(ColumnDateRange dateRange) {
		if (!dateRange.isSingleColumnRange()) {
			throw new ConqueryError.SqlConversionError("Expecting a single column range for Postgres SQL dialect.");
		}
	}

}

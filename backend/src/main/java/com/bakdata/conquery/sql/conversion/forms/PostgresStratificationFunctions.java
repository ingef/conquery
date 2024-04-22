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
			Interval.QUARTER_INTERVAL, DSL.val("3 months"),
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
		return dateTruncate(DSL.val("year"), lower(dateRange));
	}

	@Override
	public Field<Date> nextYearStart(ColumnDateRange dateRange) {
		return DSL.field(
				"{0} + {1} {2}",
				Date.class,
				dateTruncate(DSL.val("year"), upper(dateRange)),
				INTERVAL_KEYWORD,
				INTERVAL_MAP.get(Interval.ONE_YEAR_INTERVAL)
		);
	}

	@Override
	public Field<Date> yearEndQuarterAligned(ColumnDateRange dateRange) {
		Field<Integer> quarter = functionProvider.extract(DatePart.QUARTER, lower(dateRange));
		Field<Date> nextYearStart = nextYearStart(dateRange);
		return addQuarters(nextYearStart, quarter);
	}

	@Override
	public Field<Date> quarterStart(ColumnDateRange dateRange) {
		Field<Integer> quarter = functionProvider.extract(DatePart.QUARTER, lower(dateRange));
		// truncating the lower date to the start of its year and advancing by the calculated number of completed quarters from the start of that year
		return addQuarters(yearStart(dateRange), quarter);
	}

	@Override
	public Field<Date> nextQuartersStart(ColumnDateRange dateRange) {
		Field<Integer> quarter = functionProvider.extract(DatePart.QUARTER, upper(dateRange));
		return DSL.field(
				"{0} + {1} * {2} {3}",
				Date.class,
				dateTruncate(DSL.val("year"), upper(dateRange)),
				quarter,
				INTERVAL_KEYWORD,
				INTERVAL_MAP.get(Interval.QUARTER_INTERVAL)
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

	private Field<Date> dateTruncate(Field<String> field, Field<Date> date) {
		return DSL.function("date_trunc", Date.class, field, date);
	}

	private static Field<Date> addQuarters(Field<Date> start, Field<Integer> amountOfQuarters) {
		return DSL.field(
				"{0} + ({1} - 1) * {2} {3}",
				Date.class,
				start,
				amountOfQuarters,
				INTERVAL_KEYWORD,
				INTERVAL_MAP.get(Interval.QUARTER_INTERVAL)
		);
	}

	private static void checkIsSingleColumnRange(ColumnDateRange dateRange) {
		if (!dateRange.isSingleColumnRange()) {
			throw new IllegalStateException("Expecting a single column range for Postgres SQL dialect.");
		}
	}

}

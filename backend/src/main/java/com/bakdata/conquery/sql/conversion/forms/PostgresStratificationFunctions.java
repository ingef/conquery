package com.bakdata.conquery.sql.conversion.forms;

import static com.bakdata.conquery.sql.conversion.forms.FormConstants.SERIES_INDEX;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Map;

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
		return castExpressionToDate(jumpToYearStart(lower(dateRange)));
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
		return addQuarters(nextYearStart, quarter, Offset.MINUS_ONE);
	}

	@Override
	public Field<Date> quarterStart(ColumnDateRange dateRange) {
		Field<Integer> quarter = functionProvider.extract(DatePart.QUARTER, lower(dateRange));
		return addQuarters(jumpToYearStart(lower(dateRange)), quarter, Offset.MINUS_ONE);
	}

	@Override
	public Field<Date> nextQuartersStart(ColumnDateRange dateRange) {
		Field<Timestamp> yearStart = dateTruncate(DSL.val("year"), upper(dateRange));
		Field<Date> quarterEndInclusive = upper(dateRange).minus(1);
		Field<Integer> quarter = functionProvider.extract(DatePart.QUARTER, quarterEndInclusive);
		return addQuarters(yearStart, quarter, Offset.NONE);
	}

	@Override
	public Field<Integer> intSeriesField() {
		return SERIES_INDEX;
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
		Field<Integer> intSeriesField = intSeriesField();
		return multiplyByInterval(start, intervalExpression, intSeriesField, Offset.MINUS_ONE);
	}

	private Field<Date> calcEndDate(Field<Date> start, Field<String> intervalExpression) {
		Field<Integer> intSeriesField = intSeriesField();
		return multiplyByInterval(start, intervalExpression, intSeriesField, Offset.NONE);
	}

	private Field<Date> multiplyByInterval(Field<? extends java.util.Date> start, Field<String> intervalExpression, Field<Integer> amount, Offset offset) {
		Field<Integer> multiplier = amount.plus(offset.getOffset());
		Field<Timestamp> shiftedDate = DSL.field(
				"{0} + {1} {2} * {3}",
				Timestamp.class,
				start,
				INTERVAL_KEYWORD,
				intervalExpression,
				multiplier
		);
		// cast to date because we only want the date from the timestamp
		return castExpressionToDate(shiftedDate);
	}

	private Field<Timestamp> dateTruncate(Field<String> field, Field<Date> date) {
		return DSL.function("date_trunc", Timestamp.class, field, date);
	}

	private Field<Date> addQuarters(Field<? extends java.util.Date> start, Field<Integer> amountOfQuarters, Offset offset) {
		return multiplyByInterval(start, INTERVAL_MAP.get(Interval.QUARTER_INTERVAL), amountOfQuarters, offset);
	}

	private Field<Timestamp> jumpToYearStart(Field<Date> date) {
		return dateTruncate(DSL.val("year"), date);
	}

	private static Field<Date> castExpressionToDate(Field<Timestamp> shiftedDate) {
		return DSL.field("({0})::{1}", Date.class, shiftedDate, DSL.keyword("date"));
	}

	private static void checkIsSingleColumnRange(ColumnDateRange dateRange) {
		if (!dateRange.isSingleColumnRange()) {
			throw new IllegalStateException("Expecting a single column range for Postgres SQL dialect.");
		}
	}

}

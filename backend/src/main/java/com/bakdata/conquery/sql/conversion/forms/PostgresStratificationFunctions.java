package com.bakdata.conquery.sql.conversion.forms;

import static com.bakdata.conquery.sql.conversion.forms.FormConstants.SERIES_INDEX;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import com.bakdata.conquery.apiv1.query.concept.specific.temporal.TemporalSamplerFactory;
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
import org.jooq.impl.SQLDataType;

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
	public ColumnDateRange ofStartAndEnd(Field<Date> start, Field<Date> end) {
		return ColumnDateRange.of(functionProvider.daterange(start, end, "[)"));
	}

	@Override
	public Field<Date> absoluteIndexStartDate(ColumnDateRange dateRange) {
		return lower(dateRange);
	}

	@Override
	public Field<Date> lowerBoundYearStart(ColumnDateRange dateRange) {
		return castExpressionToDate(jumpToYearStart(lower(dateRange)));
	}

	@Override
	public Field<Date> upperBoundYearEnd(ColumnDateRange dateRange) {
		return DSL.field(
				"{0} + {1} {2}",
				Date.class,
				dateTruncate(DSL.val("year"), inclusiveUpper(dateRange)),
				INTERVAL_KEYWORD,
				INTERVAL_MAP.get(Interval.ONE_YEAR_INTERVAL)
		);
	}

	@Override
	public Field<Date> upperBoundYearEndQuarterAligned(ColumnDateRange dateRange) {
		Field<Integer> lowerBoundQuarter = functionProvider.extract(DatePart.QUARTER, lower(dateRange));
		Field<Date> upperBound = inclusiveUpper(dateRange);
		Field<Date> yearStartOfUpperBound = castExpressionToDate(jumpToYearStart(upperBound));
		Field<Date> yearEndQuarterAligned = addQuarters(yearStartOfUpperBound, lowerBoundQuarter, Offset.MINUS_ONE);
		// we add +1 year to the quarter aligned end if it is less than the upper bound we want to align
		return DSL.when(
						  yearEndQuarterAligned.lessThan(upperBound),
						  shiftByInterval(yearEndQuarterAligned, Interval.ONE_YEAR_INTERVAL, DSL.val(1), Offset.NONE)
				  )
				  .otherwise(yearEndQuarterAligned);
	}

	@Override
	public Field<Date> lowerBoundQuarterStart(ColumnDateRange dateRange) {
		return jumpToQuarterStart(lower(dateRange));
	}

	@Override
	protected Field<Date> jumpToQuarterStart(Field<Date> date) {
		Field<Integer> quarter = functionProvider.extract(DatePart.QUARTER, date);
		return addQuarters(jumpToYearStart(date), quarter, Offset.MINUS_ONE);
	}

	@Override
	public Field<Date> upperBoundQuarterEnd(ColumnDateRange dateRange) {
		Field<Date> inclusiveEnd = inclusiveUpper(dateRange);
		return jumpToNextQuarterStart(inclusiveEnd);
	}

	@Override
	protected Field<Date> jumpToNextQuarterStart(Field<Date> date) {
		Field<Timestamp> yearStart = dateTruncate(DSL.val("year"), date);
		Field<Integer> quarter = functionProvider.extract(DatePart.QUARTER, date);
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
	public Field<Date> indexSelectorField(TemporalSamplerFactory indexSelector, ColumnDateRange validityDate) {
		return switch (indexSelector) {
			case EARLIEST -> DSL.min(lower(validityDate));
			// upper returns the exclusive end date, we want to inclusive one, so we add -1 day
			case LATEST -> DSL.max(inclusiveUpper(validityDate));
			case RANDOM -> {
				// we calculate a random int which is in range of the date distance between upper and lower bound
				Field<Integer> dateDistanceInDays = functionProvider.dateDistance(ChronoUnit.DAYS, lower(validityDate), inclusiveUpper(validityDate));
				Field<BigDecimal> randomAmountOfDays = DSL.rand().times(dateDistanceInDays);
				Field<Integer> flooredAsInt = functionProvider.cast(DSL.floor(randomAmountOfDays), SQLDataType.INTEGER);
				// then we add this random amount (of days) to the start date
				Field<Date> randomDateInRange = functionProvider.addDays(lower(validityDate), flooredAsInt);
				// finally, we handle multiple ranges by randomizing which range we use to select a random date from
				yield functionProvider.random(randomDateInRange);
			}
		};
	}

	@Override
	public Field<Date> shiftByInterval(Field<Date> startDate, Interval interval, Field<Integer> amount, Offset offset) {
		Field<String> intervalExpression = INTERVAL_MAP.get(interval);
		return addMultipliedInterval(startDate, intervalExpression, amount, offset);
	}

	@Override
	protected Field<Date> lower(ColumnDateRange dateRange) {
		checkIsSingleColumnRange(dateRange);
		return DSL.function("lower", Date.class, dateRange.getRange());
	}

	@Override
	protected Field<Date> inclusiveUpper(ColumnDateRange dateRange) {
		checkIsSingleColumnRange(dateRange);
		return exclusiveUpper(dateRange).minus(1);
	}

	@Override
	protected Field<Date> exclusiveUpper(ColumnDateRange dateRange) {
		checkIsSingleColumnRange(dateRange);
		return DSL.function("upper", Date.class, dateRange.getRange());
	}

	@Override
	protected ColumnDateRange calcRange(Field<Date> start, Interval interval) {
		Field<String> intervalExpression = INTERVAL_MAP.get(interval);
		return ofStartAndEnd(
				calcStartDate(start, intervalExpression),
				calcEndDate(start, intervalExpression)
		);
	}

	private Field<Date> calcStartDate(Field<Date> start, Field<String> intervalExpression) {
		Field<Integer> intSeriesField = intSeriesField();
		return addMultipliedInterval(start, intervalExpression, intSeriesField, Offset.MINUS_ONE);
	}

	private Field<Date> calcEndDate(Field<Date> start, Field<String> intervalExpression) {
		Field<Integer> intSeriesField = intSeriesField();
		return addMultipliedInterval(start, intervalExpression, intSeriesField, Offset.NONE);
	}

	private Field<Date> addMultipliedInterval(Field<? extends java.util.Date> start, Field<String> intervalExpression, Field<Integer> amount, Offset offset) {
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
		return addMultipliedInterval(start, INTERVAL_MAP.get(Interval.QUARTER_INTERVAL), amountOfQuarters, offset);
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

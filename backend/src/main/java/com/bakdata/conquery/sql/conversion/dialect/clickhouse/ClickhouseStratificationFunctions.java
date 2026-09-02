package com.bakdata.conquery.sql.conversion.dialect.clickhouse;

import static com.bakdata.conquery.sql.conversion.dialect.Interval.MONTHS_PER_QUARTER;
import static com.bakdata.conquery.sql.conversion.forms.FormConstants.SERIES_INDEX;
import static org.jooq.impl.DSL.*;
import static org.jooq.impl.DSL.inline;

import java.sql.Date;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.apiv1.query.TemporalSamplerFactory;
import com.bakdata.conquery.sql.conversion.dialect.Interval;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.forms.Offset;
import com.bakdata.conquery.sql.conversion.forms.StratificationFunctions;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;

@Getter
@RequiredArgsConstructor
public class ClickhouseStratificationFunctions extends StratificationFunctions {

	private final SqlFunctionProvider functionProvider;

	private static Field<Date> addMonths(Field<Date> yearStart, Field<Integer> amount) {
		return function("addMonths", Date.class, yearStart, amount);
	}

	private static Field<Date> addDays(Field<Date> start, Field<Integer> amount) {
		return function("addDays", Date.class, start, amount);
	}

	private static Field<Date> jumpToYearStart(Field<Date> date) {
		return function("toStartOfYear", Date.class, date);
	}

	@Override
	public Field<Date> lower(ColumnDateRange dateRange) {
		return dateRange.getStart();
	}

	@Override
	protected Field<Date> inclusiveUpper(ColumnDateRange dateRange) {
		return functionProvider.addDays(exclusiveUpper(dateRange), inline(-1));
	}

	@Override
	protected Field<Date> exclusiveUpper(ColumnDateRange dateRange) {
		return dateRange.getEnd();
	}

	@Override
	protected ColumnDateRange calcRange(Field<Date> start, Interval interval) {
		return ColumnDateRange.of(
				calcStartDate(start, interval),
				calcEndDate(start, interval)
		);
	}

	@Override
	public Field<Date> absoluteIndexStartDate(ColumnDateRange dateRange) {
		return dateRange.getStart();
	}

	@Override
	public Field<Date> lowerBoundYearStart(ColumnDateRange dateRange) {
		return jumpToYearStart(dateRange.getStart());
	}

	@Override
	public Field<Date> upperBoundYearEnd(ColumnDateRange dateRange) {
		return field("addYears(toStartOfYear({0}), {1})", Date.class, dateRange.getEnd(), inline(1));
	}

	@Override
	public Field<Date> upperBoundYearEndQuarterAligned(ColumnDateRange dateRange) {
		Field<Date> yearStartOfUpperBound = jumpToYearStart(dateRange.getEnd());
		Field<Integer> quartersInMonths = getQuartersInMonths(dateRange.getStart(), Offset.MINUS_ONE);
		Field<Date> yearEndQuarterAligned = addMonths(yearStartOfUpperBound, quartersInMonths);
		// we add +1 year to the quarter aligned end if it is less than the upper bound we want to align
		return when(
						  yearEndQuarterAligned.lessThan(dateRange.getEnd()),
						  shiftByInterval(yearEndQuarterAligned, Interval.ONE_YEAR_INTERVAL, inline(1), Offset.NONE)
				  )
				  .otherwise(yearEndQuarterAligned);
	}

	@Override
	public Field<Date> lowerBoundQuarterStart(ColumnDateRange dateRange) {
		return jumpToQuarterStart(dateRange.getStart());
	}

	@Override
	public Field<Date> jumpToQuarterStart(Field<Date> date) {
		Field<Date> yearStart = jumpToYearStart(date);
		Field<Integer> quartersInMonths = getQuartersInMonths(date, Offset.MINUS_ONE);
		return addMonths(yearStart, quartersInMonths);
	}

	@Override
	public Field<Date> upperBoundQuarterEnd(ColumnDateRange dateRange) {
		return jumpToNextQuarterStart(inclusiveUpper(dateRange));
	}

	@Override
	public Field<Date> jumpToNextQuarterStart(Field<Date> date) {
		Field<Date> yearStart = jumpToYearStart(date);
		Field<Integer> quartersInMonths = getQuartersInMonths(date, Offset.NONE);
		return addMonths(yearStart, quartersInMonths);
	}

	@Override
	public Field<Integer> intSeriesField() {
		return SERIES_INDEX;
	}

	@Override
	public Table<Record> generateIntSeries(int start, int end) {
		//TODO this only supports ungigned values. Probably need to handle negatives manually Q_Q
		return table("generate_series({0}, {1}, 1)", start, end);
	}

	@Override
	public Field<Date> indexSelectorField(TemporalSamplerFactory indexSelector, ColumnDateRange validityDate) {
		return switch (indexSelector) {
			case EARLIEST -> min(validityDate.getStart());
			case LATEST -> max(inclusiveUpper(validityDate));
			case RANDOM -> {
				// we calculate a random int which is in range of the date distance between upper and lower bound
				Field<Integer> dateDistanceInDays = functionProvider.dateDistance(ChronoUnit.DAYS, validityDate.getStart(), validityDate.getEnd());
				Field<Double> randomAmountOfDays = function("RAND", Double.class).times(dateDistanceInDays);
				Field<Integer> flooredAsInt = functionProvider.cast(floor(randomAmountOfDays), SQLDataType.INTEGER);
				// then we add this random amount (of days) to the start date
				Field<Date> randomDateInRange = functionProvider.addDays(lower(validityDate), flooredAsInt);
				// finally, we handle multiple ranges by randomizing which range we use to select a random date from
				yield functionProvider.random(randomDateInRange);
			}
		};
	}

	@Override
	public Field<Date> shiftByInterval(Field<Date> startDate, Interval interval, Field<Integer> amount, Offset offset) {
		Field<Integer> multiplier = amount.plus(offset.getOffset());
		return switch (interval) {
			case ONE_YEAR_INTERVAL -> function("addYears", Date.class, startDate, multiplier.times(Interval.ONE_YEAR_INTERVAL.getAmount()));
			case YEAR_AS_DAYS_INTERVAL -> addDays(startDate, multiplier.times(Interval.YEAR_AS_DAYS_INTERVAL.getAmount()));
			case QUARTER_INTERVAL -> addMonths(startDate, multiplier.times(Interval.QUARTER_INTERVAL.getAmount()));
			case NINETY_DAYS_INTERVAL -> addDays(startDate, multiplier.times(Interval.NINETY_DAYS_INTERVAL.getAmount()));
			case ONE_DAY_INTERVAL -> addDays(startDate, multiplier.times(Interval.ONE_DAY_INTERVAL.getAmount()));
		};
	}

	private Field<Date> calcStartDate(Field<Date> start, Interval interval) {
		return calcDate(start, interval, Offset.MINUS_ONE);
	}

	private Field<Date> calcEndDate(Field<Date> start, Interval interval) {
		return calcDate(start, interval, Offset.NONE);
	}

	private Field<Date> calcDate(Field<Date> start, Interval interval, Offset offset) {
		return shiftByInterval(start, interval, intSeriesField(), offset);
	}

	private Field<Integer> getQuartersInMonths(Field<Date> date, Offset offset) {
		Field<String> quarterExpression = functionProvider.yearQuarter(date);
		Field<String> rightMostCharacter = function("RIGHT", String.class, quarterExpression, inline(1));
		Field<Integer> amountOfQuarters = functionProvider.cast(rightMostCharacter, SQLDataType.INTEGER)
														  .plus(offset.getOffset());
		return amountOfQuarters.times(MONTHS_PER_QUARTER);
	}

}

package com.bakdata.conquery.sql.conversion.forms;

import static com.bakdata.conquery.sql.conversion.dialect.Interval.MONTHS_PER_QUARTER;

import java.sql.Date;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.apiv1.query.concept.specific.temporal.TemporalSamplerFactory;
import com.bakdata.conquery.sql.conversion.dialect.HanaSqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.dialect.Interval;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

@Getter
@RequiredArgsConstructor
class HanaStratificationFunctions extends StratificationFunctions {

	private static final int INCREMENT = 1;

	/**
	 * Hana pre-generates the column names of a generated series
	 * (see <a href="https://help.sap.com/docs/hana-cloud-database/sap-hana-cloud-sap-hana-database-sql-reference-guide/series-generate-function-series-data">HANA docs</a>)
	 */
	private static final String GENERATED_PERIOD_END = "GENERATED_PERIOD_END";

	private final HanaSqlFunctionProvider functionProvider;

	@Override
	protected Field<Date> lower(ColumnDateRange dateRange) {
		// HANA does not support single-column ranges, so we can return start and end directly
		return dateRange.getStart();
	}

	@Override
	protected Field<Date> inclusiveUpper(ColumnDateRange dateRange) {
		return functionProvider.addDays(exclusiveUpper(dateRange), DSL.val(-1));
	}

	@Override
	protected Field<Date> exclusiveUpper(ColumnDateRange dateRange) {
		// HANA does not support single-column ranges, so we can return start and end directly
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
		return DSL.field(
				"SERIES_ROUND({0}, {1}, {2})",
				Date.class,
				dateRange.getEnd(),
				DSL.val("INTERVAL 1 YEAR"),
				DSL.keyword("ROUND_UP")
		);
	}

	@Override
	public Field<Date> upperBoundYearEndQuarterAligned(ColumnDateRange dateRange) {
		Field<Date> yearStartOfUpperBound = jumpToYearStart(dateRange.getEnd());
		Field<Integer> quartersInMonths = getQuartersInMonths(dateRange.getStart(), Offset.MINUS_ONE);
		Field<Date> yearEndQuarterAligned = addMonths(yearStartOfUpperBound, quartersInMonths);
		// we add +1 year to the quarter aligned end if it is less than the upper bound we want to align
		return DSL.when(
						  yearEndQuarterAligned.lessThan(dateRange.getEnd()),
						  shiftByInterval(yearEndQuarterAligned, Interval.ONE_YEAR_INTERVAL, DSL.val(1), Offset.NONE)
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
		return DSL.field(DSL.name(GENERATED_PERIOD_END), Integer.class);
	}

	@Override
	public Table<Record> generateIntSeries(int start, int end) {
		// HANA's SERIES_GENERATE_INTEGER generates an empty set if start and end are the same, but we expect it to return a set with exactly 1 value,
		// so we shift the start by -1 and use the GENERATED_PERIOD_END as intSeriesField column to achieve this
		int adjustedStart = start - 1;
		return DSL.table("SERIES_GENERATE_INTEGER({0}, {1}, {2})", INCREMENT, adjustedStart, end);
	}

	@Override
	public Field<Date> indexSelectorField(TemporalSamplerFactory indexSelector, ColumnDateRange validityDate) {
		return switch (indexSelector) {
			case EARLIEST -> DSL.min(validityDate.getStart());
			case LATEST -> DSL.max(inclusiveUpper(validityDate));
			case RANDOM -> {
				// we calculate a random int which is in range of the date distance between upper and lower bound
				Field<Integer> dateDistanceInDays = functionProvider.dateDistance(ChronoUnit.DAYS, validityDate.getStart(), validityDate.getEnd());
				Field<Double> randomAmountOfDays = DSL.function("RAND", Double.class).times(dateDistanceInDays);
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
		Field<Integer> multiplier = amount.plus(offset.getOffset());
		return switch (interval) {
			case ONE_YEAR_INTERVAL -> DSL.function("ADD_YEARS", Date.class, startDate, multiplier.times(Interval.ONE_YEAR_INTERVAL.getAmount()));
			case YEAR_AS_DAYS_INTERVAL -> addDays(startDate, multiplier.times(Interval.YEAR_AS_DAYS_INTERVAL.getAmount()));
			case QUARTER_INTERVAL -> addMonths(startDate, multiplier.times(Interval.QUARTER_INTERVAL.getAmount()));
			case NINETY_DAYS_INTERVAL -> addDays(startDate, multiplier.times(Interval.NINETY_DAYS_INTERVAL.getAmount()));
			case ONE_DAY_INTERVAL -> addDays(startDate, multiplier.times(Interval.ONE_DAY_INTERVAL.getAmount()));
		};
	}

	private static Field<Date> addMonths(Field<Date> yearStart, Field<Integer> amount) {
		return DSL.function("ADD_MONTHS", Date.class, yearStart, amount);
	}

	private static Field<Date> addDays(Field<Date> start, Field<Integer> amount) {
		return DSL.function("ADD_DAYS", Date.class, start, amount);
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

	private static Field<Date> jumpToYearStart(Field<Date> date) {
		return DSL.field(
				"SERIES_ROUND({0}, {1}, {2})",
				Date.class,
				date,
				DSL.val("INTERVAL 1 YEAR"),
				DSL.keyword("ROUND_DOWN")
		);
	}

	private Field<Integer> getQuartersInMonths(Field<Date> date, Offset offset) {
		Field<String> quarterExpression = functionProvider.yearQuarter(date);
		Field<String> rightMostCharacter = DSL.function("RIGHT", String.class, quarterExpression, DSL.val(1));
		Field<Integer> amountOfQuarters = functionProvider.cast(rightMostCharacter, SQLDataType.INTEGER)
														  .plus(offset.getOffset());
		return amountOfQuarters.times(MONTHS_PER_QUARTER);
	}

}

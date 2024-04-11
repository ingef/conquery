package com.bakdata.conquery.sql.conversion.forms;

import java.sql.Date;

import com.bakdata.conquery.sql.conversion.dialect.HanaSqlFunctionProvider;
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
	private static final String GENERATED_PERIOD_START = "GENERATED_PERIOD_START";

	private final HanaSqlFunctionProvider functionProvider;

	@Override
	protected Field<Date> lower(ColumnDateRange dateRange) {
		// HANA does not support single-column ranges, so we can return start and end directly
		return dateRange.getStart();
	}

	@Override
	protected Field<Date> upper(ColumnDateRange dateRange) {
		// HANA does not support single-column ranges, so we can return start and end directly
		return dateRange.getEnd();
	}

	@Override
	protected Field<Integer> round(Field<? extends Number> number) {
		return DSL.function("ROUND", Integer.class, number);
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
	public Field<Date> yearStart(ColumnDateRange dateRange) {
		return DSL.field(
				"SERIES_ROUND({0}, {1}, {2})",
				Date.class,
				dateRange.getStart(),
				DSL.val("INTERVAL 1 YEAR"),
				DSL.keyword("ROUND_DOWN")
		);
	}

	@Override
	public Field<Date> quarterStart(ColumnDateRange dateRange) {

		Field<Date> yearStart = yearStart(dateRange);

		Field<String> quarterExpression = functionProvider.yearQuarter(dateRange.getStart());
		Field<String> rightMostCharacter = DSL.function("RIGHT", String.class, quarterExpression, DSL.val(1));
		Field<Integer> amountOfQuarters = functionProvider.cast(rightMostCharacter, SQLDataType.INTEGER).minus(1);
		Field<Integer> quartersInMonths = amountOfQuarters.times(MONTHS_PER_QUARTER);

		return addMonths(yearStart, quartersInMonths);
	}

	@Override
	public Field<Integer> intSeriesField() {
		return DSL.field(DSL.name(GENERATED_PERIOD_START), Integer.class);
	}

	@Override
	public Table<Record> generateIntSeries(int start, int end) {
		return DSL.table("SERIES_GENERATE_INTEGER({0}, {1}, {2})", INCREMENT, start, end);
	}

	private static Field<Date> addMonths(Field<Date> yearStart, Field<Integer> amount) {
		return DSL.function("ADD_MONTHS", Date.class, yearStart, amount);
	}

	private static Field<Date> addDays(Field<Date> start, Field<Integer> amount) {
		return DSL.function("ADD_DAYS", Date.class, start, amount);
	}

	private Field<Date> calcStartDate(Field<Date> start, Interval interval) {
		return calcDate(start, interval, 1);
	}

	private Field<Date> calcEndDate(Field<Date> start, Interval interval) {
		return calcDate(start, interval, 0);
	}

	private Field<Date> calcDate(Field<Date> start, Interval interval, int offset) {
		Field<Integer> seriesIndex = intSeriesField().minus(offset);
		return switch (interval) {
			case ONE_YEAR_INTERVAL -> DSL.function("ADD_YEARS", Date.class, start, seriesIndex.times(Interval.ONE_YEAR_INTERVAL.getAmount()));
			case YEAR_AS_DAYS_INTERVAL -> addDays(start, seriesIndex.times(Interval.YEAR_AS_DAYS_INTERVAL.getAmount()));
			case THREE_MONTHS_INTERVAL -> addMonths(start, seriesIndex.times(Interval.THREE_MONTHS_INTERVAL.getAmount()));
			case NINETY_DAYS_INTERVAL -> addDays(start, seriesIndex.times(Interval.NINETY_DAYS_INTERVAL.getAmount()));
			case ONE_DAY_INTERVAL -> addDays(start, seriesIndex.times(Interval.ONE_DAY_INTERVAL.getAmount()));
		};
	}

}

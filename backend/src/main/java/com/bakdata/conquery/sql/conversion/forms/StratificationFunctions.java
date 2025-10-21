package com.bakdata.conquery.sql.conversion.forms;

import static com.bakdata.conquery.sql.conversion.dialect.Interval.DAYS_PER_QUARTER;
import static com.bakdata.conquery.sql.conversion.dialect.Interval.DAYS_PER_YEAR;
import static com.bakdata.conquery.sql.conversion.dialect.Interval.MONTHS_PER_QUARTER;
import static com.bakdata.conquery.sql.conversion.forms.FormConstants.DAY_ALIGNED_COUNT;
import static com.bakdata.conquery.sql.conversion.forms.FormConstants.INDEX_SELECTOR;
import static com.bakdata.conquery.sql.conversion.forms.FormConstants.INDEX_START;
import static com.bakdata.conquery.sql.conversion.forms.FormConstants.QUARTER_ALIGNED_COUNT;
import static com.bakdata.conquery.sql.conversion.forms.FormConstants.QUARTER_END;
import static com.bakdata.conquery.sql.conversion.forms.FormConstants.QUARTER_START;
import static com.bakdata.conquery.sql.conversion.forms.FormConstants.YEAR_ALIGNED_COUNT;
import static com.bakdata.conquery.sql.conversion.forms.FormConstants.YEAR_END;
import static com.bakdata.conquery.sql.conversion.forms.FormConstants.YEAR_END_QUARTER_ALIGNED;
import static com.bakdata.conquery.sql.conversion.forms.FormConstants.YEAR_START;

import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.TemporalSamplerFactory;
import com.bakdata.conquery.models.forms.util.CalendarUnit;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.HanaSqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.dialect.Interval;
import com.bakdata.conquery.sql.conversion.dialect.PostgreSqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

public abstract class StratificationFunctions {

	public static StratificationFunctions create(ConversionContext context) {
		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();
		return switch (context.getConfig().getDialect()) {
			case POSTGRESQL -> new PostgresStratificationFunctions((PostgreSqlFunctionProvider) functionProvider);
			case HANA -> new HanaStratificationFunctions((HanaSqlFunctionProvider) functionProvider);
		};
	}

	public ColumnDateRange ofStartAndEnd(Field<Date> start, Field<Date> end) {
		return ColumnDateRange.of(start, end); // needs to be overwritten for dialects that support single-column ranges
	}

	protected abstract SqlFunctionProvider getFunctionProvider();

	/**
	 * Extract the lower bounds from a given daterange.
	 */
	public abstract Field<Date> lower(ColumnDateRange dateRange);

	/**
	 * Extract the inclusive upper bound from a given daterange.
	 */
	protected abstract Field<Date> inclusiveUpper(ColumnDateRange dateRange);

	/**
	 * Extract the exclusive upper bound from a given daterange.
	 */
	protected abstract Field<Date> exclusiveUpper(ColumnDateRange dateRange);

	/**
	 * Calculates the start and end date based on the given start date and an interval expression.
	 */
	protected abstract ColumnDateRange calcRange(Field<Date> start, Interval interval);

	/**
	 * Extracts the absolute index start date by using the lower bound of the provided date range.
	 */
	public abstract Field<Date> absoluteIndexStartDate(ColumnDateRange dateRange);

	/**
	 * Determines the start of the year based on the lower bound of the provided date range.
	 */
	public abstract Field<Date> lowerBoundYearStart(ColumnDateRange dateRange);

	/**
	 * Determines the exclusive end (first day of the next year) of the upper bound of the provided date range.
	 */
	public abstract Field<Date> upperBoundYearEnd(ColumnDateRange dateRange);

	/**
	 * Determines the end of the upper bound of the provided date range, but aligned on the quarter of the lower bound of the
	 * provided daterange.
	 */
	public abstract Field<Date> upperBoundYearEndQuarterAligned(ColumnDateRange dateRange);

	/**
	 * Calculates the start of the quarter using the lower bound of the provided date range.
	 */
	public abstract Field<Date> lowerBoundQuarterStart(ColumnDateRange dateRange);

	/**
	 * Calculates the start of the quarter of the given date.
	 */
	public abstract Field<Date> jumpToQuarterStart(Field<Date> date);

	/**
	 * Calculates the exclusive end (first day of the next quarter) of the upper bound of the provided date range.
	 */
	public abstract Field<Date> upperBoundQuarterEnd(ColumnDateRange dateRange);

	/**
	 * Calculates the start of the next quarter of the given date.
	 */
	public abstract Field<Date> jumpToNextQuarterStart(Field<Date> date);

	/**
	 * The int field generated by the {@link #generateIntSeries(int, int)}
	 */
	public abstract Field<Integer> intSeriesField();

	/**
	 * Generates a series of integers from the start value to the exclusive end value.
	 */
	public abstract Table<Record> generateIntSeries(int start, int end);

	/**
	 * Generates a date field representing the {@link TemporalSamplerFactory} using the given validity date range.
	 */
	public abstract Field<Date> indexSelectorField(TemporalSamplerFactory indexSelector, ColumnDateRange validityDate);

	/**
	 * Shift's a start date by an interval times an amount. The offset will we added to the amount before multiplying.
	 */
	public abstract Field<Date> shiftByInterval(Field<Date> startDate, Interval interval, Field<Integer> amount, Offset offset);

	/**
	 * Generates the start and end field for the respective {@link IndexPlacement} and {@link CalendarUnit timeUnit}.
	 */
	public List<Field<Date>> indexStartFields(IndexPlacement indexPlacement, CalendarUnit timeUnit) {

		Field<Date> positiveStart;
		Field<Date> negativeStart;

		switch (timeUnit) {
			case QUARTERS -> {
				switch (indexPlacement) {
					case BEFORE -> {
						Field<Date> nextQuarterStart = jumpToNextQuarterStart(INDEX_SELECTOR);
						positiveStart = nextQuarterStart;
						negativeStart = nextQuarterStart;
					}
					case AFTER -> {
						Field<Date> quarterStart = jumpToQuarterStart(INDEX_SELECTOR);
						positiveStart = quarterStart;
						negativeStart = quarterStart;
					}
					case NEUTRAL -> {
						positiveStart = jumpToNextQuarterStart(INDEX_SELECTOR);
						negativeStart = jumpToQuarterStart(INDEX_SELECTOR);
					}
					default -> throw new CombinationNotSupportedException(indexPlacement, timeUnit);
				}
			}
			case DAYS -> {
				switch (indexPlacement) {
					case BEFORE, AFTER -> {
						positiveStart = INDEX_SELECTOR;
						negativeStart = INDEX_SELECTOR;
					}
					case NEUTRAL -> {
						positiveStart = getFunctionProvider().addDays(INDEX_SELECTOR, DSL.val(1));
						negativeStart = INDEX_SELECTOR;
					}
					default -> throw new CombinationNotSupportedException(indexPlacement, timeUnit);
				}
			}
			default -> throw new CombinationNotSupportedException(indexPlacement, timeUnit);
		}

		return List.of(
				positiveStart.as(SharedAliases.INDEX_START_POSITIVE.getAlias()),
				negativeStart.as(SharedAliases.INDEX_START_NEGATIVE.getAlias())
		);
	}

	/**
	 * Calculates the count of the required resolution windows based on the provided resolution, alignment, and date range.
	 *
	 * @return A {@link Field<Integer>} representing the count of resolution windows.
	 * @throws CombinationNotSupportedException if the combination of resolution and alignment is not supported.
	 */
	public Field<Integer> calculateResolutionWindowCount(ExportForm.ResolutionAndAlignment resolutionAndAlignment, ColumnDateRange bounds) {
		SqlFunctionProvider functionProvider = getFunctionProvider();
		return switch (resolutionAndAlignment.getResolution()) {
			case COMPLETE -> DSL.val(1);
			case YEARS -> calculateResolutionWindowForYearResolution(resolutionAndAlignment, bounds, functionProvider);
			case QUARTERS -> calculateResolutionWindowForQuarterResolution(resolutionAndAlignment, bounds, functionProvider);
			case DAYS -> functionProvider.dateDistance(ChronoUnit.DAYS, lower(bounds), exclusiveUpper(bounds))
										 .as(SharedAliases.DAY_ALIGNED_COUNT.getAlias());
		};
	}

	/**
	 * Determines the stratification range based on resolution and alignment parameters. The created stratification range is bound by the given range.
	 */
	public ColumnDateRange createStratificationRange(ExportForm.ResolutionAndAlignment resolutionAndAlignment, ColumnDateRange bounds) {

		SqlFunctionProvider functionProvider = getFunctionProvider();

		ColumnDateRange stratificationRange = switch (resolutionAndAlignment.getResolution()) {
			case COMPLETE -> bounds;
			case YEARS -> createStratificationRangeForYearResolution(resolutionAndAlignment);
			case QUARTERS -> createStratificationRangeForQuarterResolution(resolutionAndAlignment);
			case DAYS -> calcRange(INDEX_START, Interval.ONE_DAY_INTERVAL);
		};

		return functionProvider.intersection(stratificationRange, bounds).as(bounds.getAlias());
	}

	/**
	 * The index field for the corresponding resolution index {@link ConqueryConstants#CONTEXT_INDEX_INFO}.
	 */
	public Field<Integer> index(SqlIdColumns ids, Optional<ColumnDateRange> stratificationBounds) {

		List<Field<?>> partitioningFields =
				Stream.concat(
							  ids.toFields().stream(),
							  stratificationBounds.stream().flatMap(columnDateRange -> columnDateRange.toFields().stream())
					  )
					  .collect(Collectors.toList());

		return DSL.rowNumber()
				  .over(DSL.partitionBy(partitioningFields))
				  .as(SharedAliases.INDEX.getAlias());
	}

	/**
	 * Generates a condition to limit the resolution window count in a query.
	 * This method applies a check to ensure the series index does not exceed the maximum window count based on the given resolution and alignment.
	 */
	public Condition stopOnMaxResolutionWindowCount(ExportForm.ResolutionAndAlignment resolutionAndAlignment) {
		Field<Integer> seriesIndex = intSeriesField();
		return switch (resolutionAndAlignment.getResolution()) {
			case COMPLETE -> DSL.noCondition();
			case YEARS -> windowCountForYearResolution(resolutionAndAlignment, seriesIndex);
			case QUARTERS -> windowCountForQuarterResolution(resolutionAndAlignment, seriesIndex);
			case DAYS -> seriesIndex.lessOrEqual(DAY_ALIGNED_COUNT);
		};
	}

	private Field<Integer> calculateResolutionWindowForQuarterResolution(
			ExportForm.ResolutionAndAlignment resolutionAndAlignment,
			ColumnDateRange bounds,
			SqlFunctionProvider functionProvider
	) {
		return switch (resolutionAndAlignment.getAlignment()) {
			case QUARTER -> functionProvider.dateDistance(ChronoUnit.MONTHS, QUARTER_START, QUARTER_END)
											.divide(MONTHS_PER_QUARTER)
											.as(SharedAliases.QUARTER_ALIGNED_COUNT.getAlias());
			case DAY -> functionProvider.dateDistance(ChronoUnit.DAYS, lower(bounds), exclusiveUpper(bounds))
										.plus(89)
										.divide(DAYS_PER_QUARTER)
										.as(SharedAliases.DAY_ALIGNED_COUNT.getAlias());
			default -> throw new CombinationNotSupportedException(resolutionAndAlignment);
		};
	}

	private Field<Integer> calculateResolutionWindowForYearResolution(
			ExportForm.ResolutionAndAlignment resolutionAndAlignment,
			ColumnDateRange bounds,
			SqlFunctionProvider functionProvider
	) {
		return switch (resolutionAndAlignment.getAlignment()) {
			case YEAR -> functionProvider.dateDistance(ChronoUnit.YEARS, YEAR_START, YEAR_END)
										 .as(SharedAliases.YEAR_ALIGNED_COUNT.getAlias());
			case QUARTER -> functionProvider.dateDistance(ChronoUnit.YEARS, QUARTER_START, YEAR_END_QUARTER_ALIGNED)
											.as(SharedAliases.QUARTER_ALIGNED_COUNT.getAlias());
			case DAY -> functionProvider.dateDistance(ChronoUnit.DAYS, lower(bounds), exclusiveUpper(bounds))
										.plus(364)
										.divide(DAYS_PER_YEAR)
										.as(SharedAliases.DAY_ALIGNED_COUNT.getAlias());
			default -> throw new CombinationNotSupportedException(resolutionAndAlignment);
		};
	}

	private ColumnDateRange createStratificationRangeForQuarterResolution(ExportForm.ResolutionAndAlignment resolutionAndAlignment) {
		return switch (resolutionAndAlignment.getAlignment()) {
			case QUARTER -> calcRange(QUARTER_START, Interval.QUARTER_INTERVAL);
			case DAY -> calcRange(INDEX_START, Interval.NINETY_DAYS_INTERVAL);
			default -> throw new CombinationNotSupportedException(resolutionAndAlignment);
		};
	}

	private ColumnDateRange createStratificationRangeForYearResolution(ExportForm.ResolutionAndAlignment resolutionAndAlignment) {
		return switch (resolutionAndAlignment.getAlignment()) {
			case YEAR -> calcRange(YEAR_START, Interval.ONE_YEAR_INTERVAL);
			case QUARTER -> calcRange(QUARTER_START, Interval.ONE_YEAR_INTERVAL);
			case DAY -> calcRange(INDEX_START, Interval.YEAR_AS_DAYS_INTERVAL);
			default -> throw new CombinationNotSupportedException(resolutionAndAlignment);
		};
	}

	private static Condition windowCountForQuarterResolution(ExportForm.ResolutionAndAlignment resolutionAndAlignment, Field<Integer> seriesIndex) {
		return switch (resolutionAndAlignment.getAlignment()) {
			case QUARTER -> seriesIndex.lessOrEqual(QUARTER_ALIGNED_COUNT);
			case DAY -> seriesIndex.lessOrEqual(DAY_ALIGNED_COUNT);
			default -> throw new CombinationNotSupportedException(resolutionAndAlignment);
		};
	}

	private static Condition windowCountForYearResolution(ExportForm.ResolutionAndAlignment resolutionAndAlignment, Field<Integer> seriesIndex) {
		return switch (resolutionAndAlignment.getAlignment()) {
			case YEAR -> seriesIndex.lessOrEqual(YEAR_ALIGNED_COUNT);
			case QUARTER -> seriesIndex.lessOrEqual(QUARTER_ALIGNED_COUNT);
			case DAY -> seriesIndex.lessOrEqual(DAY_ALIGNED_COUNT);
			default -> throw new CombinationNotSupportedException(resolutionAndAlignment);
		};
	}

}

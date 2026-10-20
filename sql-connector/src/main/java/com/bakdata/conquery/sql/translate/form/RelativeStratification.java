package com.bakdata.conquery.sql.translate.form;

import static com.bakdata.conquery.sql.translate.form.FormConstants.INDEX_SELECTOR;
import static com.bakdata.conquery.sql.translate.form.FormConstants.INDEX_START_NEGATIVE;
import static com.bakdata.conquery.sql.translate.form.FormConstants.INDEX_START_POSITIVE;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.Selects;
import com.bakdata.conquery.sql.translate.ir.SharedAliases;
import com.bakdata.conquery.sql.translate.ir.SqlIdColumns;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.model.form.FormCalendarUnit;
import com.bakdata.conquery.sql.model.form.FormResolution;
import com.bakdata.conquery.sql.model.form.RelativeFormSettings;
import com.bakdata.conquery.sql.model.form.ResolutionAndAlignment;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
class RelativeStratification {

	private final QueryStep baseStep;
	private final StratificationFunctions stratificationFunctions;
	private final TranslatorDialect dialect;

	public QueryStep createRelativeStratificationTable(RelativeFormSettings form, boolean negate) {

		//TODO why does this not have a conversion context?

		// we want to create the stratification for each distinct validity date range of an entity,
		// so we first need to unnest the validity date in case it is a multirange
		if (baseStep.getSelects().getValidityDate().isEmpty()) {
			throw new IllegalArgumentException("Base step must contain a validity date");
		}
		String unnestCteName = FormCteStep.UNNEST_DATES.getSuffix();
		QueryStep withUnnestedValidityDate = dialect.supportsSingleColumnRanges()
				? dialect.unnestDateRange(baseStep.getSelects().getValidityDate().get(), baseStep, unnestCteName)
				: baseStep;

		QueryStep indexSelectorStep = createIndexSelectorStep(form, withUnnestedValidityDate);
		QueryStep indexStartStep = createIndexStartStep(form, indexSelectorStep);
		QueryStep totalBoundsStep = createTotalBoundsStep(form, indexStartStep);

		List<QueryStep> tables = form.resolutions().stream()
									 .map(ResolutionAndAlignment::resolution)
									 .map(resolution -> createResolutionTable(totalBoundsStep, resolution, form, negate))
									 .toList();

		List<QueryStep> predecessors = new ArrayList<>();
		predecessors.add(baseStep);
		if (baseStep != withUnnestedValidityDate) {
			predecessors.add(withUnnestedValidityDate);
		}
		predecessors.addAll(List.of(indexSelectorStep, indexStartStep, totalBoundsStep));

		return StratificationTableFactory.unionResolutionTables(tables, predecessors, negate);
	}

	/**
	 * Creates a {@link QueryStep} containing the selected index date of the relative form.
	 */
	private QueryStep createIndexSelectorStep(RelativeFormSettings form, QueryStep prerequisite) {

		Selects predecessorSelects = prerequisite.getQualifiedSelects();
		ColumnDateRange validityDate = predecessorSelects.getValidityDate()
														 .orElseThrow(() -> new IllegalStateException("Expecting a validity date to be present"));
		Field<Date> indexDate = stratificationFunctions.indexSelectorField(form.indexSelector(), validityDate)
													   .as(SharedAliases.INDEX_SELECTOR.getAlias());

		Selects selects = Selects.builder()
								 .ids(predecessorSelects.getIds())
								 .sqlSelect(new FieldWrapper<>(indexDate))
								 .build();

		return QueryStep.builder()
						.cteName(FormCteStep.INDEX_SELECTOR.getSuffix())
						.selects(selects)
						.fromTable(QueryStep.toTableLike(prerequisite.getCteName()))
						.groupBy(predecessorSelects.getIds().toFields())
						.build();
	}

	/**
	 * Creates {@link QueryStep} containing the start date selects ({@link FormConstants#INDEX_START_POSITIVE} and {@link FormConstants#INDEX_START_NEGATIVE})
	 * from where the feature and/or outcome ranges of the relative form start.
	 */
	private QueryStep createIndexStartStep(RelativeFormSettings form, QueryStep indexSelectorStep) {

		List<FieldWrapper<Date>> indexStartFields = stratificationFunctions.indexStartFields(form.indexPlacement(), form.timeUnit()).stream()
																		   .map(FieldWrapper::new)
																		   .toList();

		// add index start fields to qualified selects of previous step
		Selects selects = indexSelectorStep.getQualifiedSelects()
										   .toBuilder()
										   .sqlSelects(indexStartFields)
										   .build();

		return QueryStep.builder()
						.cteName(FormCteStep.INDEX_START.getSuffix())
						.selects(selects)
						.fromTable(QueryStep.toTableLike(indexSelectorStep.getCteName()))
						.build();
	}

	/**
	 * Creates a {@link QueryStep} containing the minimum and maximum stratification date for each entity.
	 */
	private QueryStep createTotalBoundsStep(RelativeFormSettings form, QueryStep indexStartStep) {

		Interval interval = getInterval(form.timeUnit(), FormResolution.COMPLETE);
		IntRange intRange = toGenerateSeriesBounds(form, FormResolution.COMPLETE);

		Field<Date> minStratificationDate = stratificationFunctions.shiftByInterval(INDEX_START_NEGATIVE, interval, DSL.inline(intRange.min()), Offset.NONE);
		Field<Date> maxStratificationDate = stratificationFunctions.shiftByInterval(INDEX_START_POSITIVE, interval, DSL.inline(intRange.max()), Offset.NONE);
		ColumnDateRange minAndMaxStratificationDate = stratificationFunctions.ofStartAndEnd(minStratificationDate, maxStratificationDate)
																			 .as(SharedAliases.STRATIFICATION_BOUNDS.getAlias());

		// add min and max stratification date to qualified selects of previous step
		Selects selects = indexStartStep.getQualifiedSelects()
										.toBuilder()
										.stratificationDate(Optional.of(minAndMaxStratificationDate))
										.build();

		return QueryStep.builder()
						.cteName(FormCteStep.TOTAL_BOUNDS.getSuffix())
						.selects(selects)
						.fromTable(QueryStep.toTableLike(indexStartStep.getCteName()))
						.build();
	}

	private QueryStep createResolutionTable(QueryStep indexStartStep, FormResolution resolution, RelativeFormSettings form, boolean negate) {
		return switch (resolution) {
			case COMPLETE -> createCompleteTable(indexStartStep, form, negate);
			case YEARS, QUARTERS, DAYS -> createIntervalTable(indexStartStep, resolution, form, negate);
		};
	}

	private QueryStep createCompleteTable(QueryStep totalBoundsStep, RelativeFormSettings form, boolean negate) {

		Selects predecessorSelects = totalBoundsStep.getQualifiedSelects();
		Interval interval = getInterval(form.timeUnit(), FormResolution.COMPLETE);
		IntRange intRange = toGenerateSeriesBounds(form, FormResolution.COMPLETE);

		QueryStep featureTable = form.timeCountBefore() > 0 ? createCompleteFeatureTable(predecessorSelects, interval, intRange, totalBoundsStep) : null;
		QueryStep outcomeTable = form.timeCountAfter() > 0 ? createCompleteOutcomeTable(predecessorSelects, interval, intRange, totalBoundsStep) : null;

		return QueryStep.createUnionAllStep(
				Stream.concat(Stream.ofNullable(outcomeTable), Stream.ofNullable(featureTable)).toList(),
				FormCteStep.COMPLETE.getSuffix(),
				Collections.emptyList(),
				negate
		);
	}

	private QueryStep createCompleteFeatureTable(Selects predecessorSelects, Interval interval, IntRange intRange, QueryStep totalBoundsStep) {
		Field<Integer> featureIndex = DSL.field(DSL.inline(-1)).as(SharedAliases.INDEX.getAlias());
		SqlIdColumns featureIds = predecessorSelects.getIds().withStratification(FormResolution.COMPLETE.name(), featureIndex, INDEX_SELECTOR);
		Field<Date> rangeStart = stratificationFunctions.shiftByInterval(INDEX_START_NEGATIVE, interval, DSL.inline(intRange.min()), Offset.NONE);
		return createIntervalStep(featureIds, rangeStart, INDEX_START_NEGATIVE, Optional.empty(), totalBoundsStep);
	}

	private QueryStep createCompleteOutcomeTable(Selects predecessorSelects, Interval interval, IntRange intRange, QueryStep totalBoundsStep) {
		Field<Integer> outcomeIndex = DSL.field(DSL.inline(1)).as(SharedAliases.INDEX.getAlias());
		SqlIdColumns outcomeIds = predecessorSelects.getIds().withStratification(FormResolution.COMPLETE.name(), outcomeIndex, INDEX_SELECTOR);
		Field<Date> rangeEnd = stratificationFunctions.shiftByInterval(INDEX_START_POSITIVE, interval, DSL.inline(intRange.max()), Offset.NONE);
		return createIntervalStep(outcomeIds, INDEX_START_POSITIVE, rangeEnd, Optional.empty(), totalBoundsStep);
	}

	private QueryStep createIntervalTable(QueryStep totalBoundsStep, FormResolution resolution, RelativeFormSettings form, boolean negate) {

		Field<Integer> seriesIndex = stratificationFunctions.intSeriesField();
		Selects predecessorSelects = totalBoundsStep.getQualifiedSelects();
		SqlIdColumns ids = predecessorSelects.getIds().withStratification(resolution.name(), seriesIndex, INDEX_SELECTOR);
		Interval interval = getInterval(form.timeUnit(), resolution);
		IntRange bounds = toGenerateSeriesBounds(form, resolution);

		QueryStep timeBeforeStep = createFeatureTable(totalBoundsStep, interval, seriesIndex, bounds, ids);
		QueryStep timeAfterStep = createOutcomeTable(totalBoundsStep, interval, seriesIndex, bounds, ids);

		return QueryStep.createUnionAllStep(
				List.of(timeBeforeStep, timeAfterStep),
				FormCteStep.stratificationCte(resolution).getSuffix(),
				Collections.emptyList(),
				negate
		);
	}

	private QueryStep createOutcomeTable(QueryStep totalBoundsStep, Interval interval, Field<Integer> seriesIndex, IntRange bounds, SqlIdColumns ids) {
		Field<Date> outcomeRangeStart = stratificationFunctions.shiftByInterval(INDEX_START_POSITIVE, interval, seriesIndex, Offset.MINUS_ONE);
		Field<Date> outcomeRangeEnd = stratificationFunctions.shiftByInterval(INDEX_START_POSITIVE, interval, seriesIndex, Offset.NONE);
		Table<? extends Record> outcomeSeries = stratificationFunctions.generateIntSeries(1, bounds.max()).as(SharedAliases.INDEX.getAlias());
		return createIntervalStep(ids, outcomeRangeStart, outcomeRangeEnd, Optional.of(outcomeSeries), totalBoundsStep);
	}

	private QueryStep createFeatureTable(QueryStep totalBoundsStep, Interval interval, Field<Integer> seriesIndex, IntRange bounds, SqlIdColumns ids) {
		Field<Date> featureRangeStart = stratificationFunctions.shiftByInterval(INDEX_START_NEGATIVE, interval, seriesIndex, Offset.NONE);
		Field<Date> featureRangeEnd = stratificationFunctions.shiftByInterval(INDEX_START_NEGATIVE, interval, seriesIndex, Offset.ONE);
		Table<? extends Record> featureSeries = stratificationFunctions.generateIntSeries(bounds.min(), -1).as(SharedAliases.INDEX.getAlias());
		return createIntervalStep(ids, featureRangeStart, featureRangeEnd, Optional.of(featureSeries), totalBoundsStep);
	}

	private QueryStep createIntervalStep(
			SqlIdColumns ids,
			Field<Date> rangeStart,
			Field<Date> rangeEnd,
			Optional<Table<? extends Record>> seriesTable,
			QueryStep predecessor
	) {
		if (predecessor.getSelects().getStratificationDate().isEmpty()) {
			throw new IllegalArgumentException("Expecting predecessor to contain the min and max stratification bounds");
		}
		ColumnDateRange finalRange = dialect.intersection(
															 stratificationFunctions.ofStartAndEnd(rangeStart, rangeEnd),
															 predecessor.getQualifiedSelects().getStratificationDate().get()
													 )
													 .as(SharedAliases.STRATIFICATION_BOUNDS.getAlias());

		Selects selects = Selects.builder()
								 .ids(ids)
								 .stratificationDate(Optional.of(finalRange))
								 .build();

		QueryStep.QueryStepBuilder queryStep = QueryStep.builder()
														.selects(selects)
														.fromTable(QueryStep.toTableLike(predecessor.getCteName()));

		seriesTable.ifPresent(queryStep::fromTable);
		return queryStep.build();
	}

	/**
	 * Adjusts the before and after bounds so they fit the SQL approach.
	 * Take time unit QUARTERS and Resolution YEARS as an example: If the time counts are not divisible by 4 (because 1 year == 4 quarters), we need to round
	 * up for each starting year. 5 Quarters mean 2 years we have to consider when creating the stratification.
	 */
	private static IntRange toGenerateSeriesBounds(RelativeFormSettings relativeForm, FormResolution resolution) {

		int timeCountBefore;
		int timeCountAfter;

		switch (relativeForm.timeUnit()) {
			case QUARTERS -> {
				if (resolution == FormResolution.YEARS) {
					timeCountBefore = divideAndRoundUp(relativeForm.timeCountBefore(), 4);
					timeCountAfter = divideAndRoundUp(relativeForm.timeCountAfter(), 4);
				}
				else {
					timeCountBefore = relativeForm.timeCountBefore();
					timeCountAfter = relativeForm.timeCountAfter();
				}
			}
			case DAYS -> {
				switch (resolution) {
					case COMPLETE, DAYS -> {
						timeCountBefore = relativeForm.timeCountBefore();
						timeCountAfter = relativeForm.timeCountAfter();
					}
					case YEARS -> {
						timeCountBefore = divideAndRoundUp(relativeForm.timeCountBefore(), Interval.YEAR_AS_DAYS_INTERVAL.getAmount());
						timeCountAfter = divideAndRoundUp(relativeForm.timeCountAfter(), Interval.YEAR_AS_DAYS_INTERVAL.getAmount());
					}
					case QUARTERS -> {
						timeCountBefore = divideAndRoundUp(relativeForm.timeCountBefore(), Interval.NINETY_DAYS_INTERVAL.getAmount());
						timeCountAfter = divideAndRoundUp(relativeForm.timeCountAfter(), Interval.NINETY_DAYS_INTERVAL.getAmount());
					}
					default -> throw new CombinationNotSupportedException(relativeForm.timeUnit(), resolution);
				}
			}
			default -> throw new CombinationNotSupportedException(relativeForm.timeUnit(), resolution);
		}

		return new IntRange(
				-timeCountBefore,
				timeCountAfter
		);
	}

	private static int divideAndRoundUp(int numerator, int denominator) {
		if (denominator == 0) {
			throw new IllegalArgumentException("Denominator cannot be zero.");
		}
		return (int) Math.ceil((double) numerator / denominator);
	}

	/**
	 * @return The interval expression which will be multiplied by the {@link StratificationFunctions#intSeriesField()} and added to the
	 * {@link SharedAliases#INDEX_START_NEGATIVE} or {@link SharedAliases#INDEX_START_POSITIVE}.
	 */
	private static Interval getInterval(FormCalendarUnit timeUnit, FormResolution resolution) {
		return switch (timeUnit) {
			case QUARTERS -> switch (resolution) {
				case COMPLETE, QUARTERS -> Interval.QUARTER_INTERVAL;
				case YEARS -> Interval.ONE_YEAR_INTERVAL;
				case DAYS -> Interval.ONE_DAY_INTERVAL;
			};
			case DAYS -> switch (resolution) {
				case COMPLETE, DAYS -> Interval.ONE_DAY_INTERVAL;
				case YEARS -> Interval.YEAR_AS_DAYS_INTERVAL;
				case QUARTERS -> Interval.NINETY_DAYS_INTERVAL;
			};
			default -> throw new CombinationNotSupportedException(timeUnit, resolution);
		};
	}

	private record IntRange(int min, int max) {
	}

}

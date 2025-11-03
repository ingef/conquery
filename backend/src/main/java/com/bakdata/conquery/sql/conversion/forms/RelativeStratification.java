package com.bakdata.conquery.sql.conversion.forms;

import static com.bakdata.conquery.sql.conversion.forms.FormConstants.INDEX_SELECTOR;
import static com.bakdata.conquery.sql.conversion.forms.FormConstants.INDEX_START_NEGATIVE;
import static com.bakdata.conquery.sql.conversion.forms.FormConstants.INDEX_START_POSITIVE;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.TemporalSamplerFactory;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.models.forms.util.CalendarUnit;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.dialect.Interval;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

@RequiredArgsConstructor
class RelativeStratification {

	private final QueryStep baseStep;
	private final StratificationFunctions stratificationFunctions;
	private final SqlFunctionProvider functionProvider;

	public QueryStep createRelativeStratificationTable(RelativeFormQuery form) {

		// we want to create the stratification for each distinct validity date range of an entity,
		// so we first need to unnest the validity date in case it is a multirange
		Preconditions.checkArgument(baseStep.getSelects().getValidityDate().isPresent(), "Base step must contain a validity date");
		String unnestCteName = FormCteStep.UNNEST_DATES.getSuffix();
		QueryStep withUnnestedValidityDate = functionProvider.unnestDaterange(baseStep.getSelects().getValidityDate().get(), baseStep, unnestCteName);

		QueryStep indexSelectorStep = createIndexSelectorStep(form, withUnnestedValidityDate);
		QueryStep indexStartStep = createIndexStartStep(form, indexSelectorStep);
		QueryStep totalBoundsStep = createTotalBoundsStep(form, indexStartStep);

		List<QueryStep> tables = form.getResolutionsAndAlignmentMap().stream()
									 .map(ExportForm.ResolutionAndAlignment::getResolution)
									 .map(resolution -> createResolutionTable(totalBoundsStep, resolution, form))
									 .toList();

		List<QueryStep> predecessors = new ArrayList<>();
		predecessors.add(baseStep);
		if (baseStep != withUnnestedValidityDate) {
			predecessors.add(withUnnestedValidityDate);
		}
		predecessors.addAll(List.of(indexSelectorStep, indexStartStep, totalBoundsStep));

		return StratificationTableFactory.unionResolutionTables(tables, predecessors);
	}

	/**
	 * Creates {@link QueryStep} containing the date select for the corresponding {@link TemporalSamplerFactory} of the relative form.
	 */
	private QueryStep createIndexSelectorStep(RelativeFormQuery form, QueryStep prerequisite) {

		Selects predecessorSelects = prerequisite.getQualifiedSelects();
		ColumnDateRange validityDate = predecessorSelects.getValidityDate()
														 .orElseThrow(() -> new IllegalStateException("Expecting a validity date to be present"));
		Field<Date> indexDate = stratificationFunctions.indexSelectorField(form.getIndexSelector(), validityDate)
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
	 * from where the feature and/or outcome ranges of the relative form start. Their placement depends on the relative forms {@link IndexPlacement}.
	 */
	private QueryStep createIndexStartStep(RelativeFormQuery form, QueryStep indexSelectorStep) {

		List<FieldWrapper<Date>> indexStartFields = stratificationFunctions.indexStartFields(form.getIndexPlacement(), form.getTimeUnit()).stream()
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
	private QueryStep createTotalBoundsStep(RelativeFormQuery form, QueryStep indexStartStep) {

		Interval interval = getInterval(form.getTimeUnit(), Resolution.COMPLETE);
		Range<Integer> intRange = toGenerateSeriesBounds(form, Resolution.COMPLETE);

		Field<Date> minStratificationDate = stratificationFunctions.shiftByInterval(INDEX_START_NEGATIVE, interval, DSL.val(intRange.getMin()), Offset.NONE);
		Field<Date> maxStratificationDate = stratificationFunctions.shiftByInterval(INDEX_START_POSITIVE, interval, DSL.val(intRange.getMax()), Offset.NONE);
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

	private QueryStep createResolutionTable(QueryStep indexStartStep, Resolution resolution, RelativeFormQuery form) {
		return switch (resolution) {
			case COMPLETE -> createCompleteTable(indexStartStep, form);
			case YEARS, QUARTERS, DAYS -> createIntervalTable(indexStartStep, resolution, form);
		};
	}

	private QueryStep createCompleteTable(QueryStep totalBoundsStep, RelativeFormQuery form) {

		Selects predecessorSelects = totalBoundsStep.getQualifiedSelects();
		Interval interval = getInterval(form.getTimeUnit(), Resolution.COMPLETE);
		Range<Integer> intRange = toGenerateSeriesBounds(form, Resolution.COMPLETE);

		QueryStep featureTable = form.getTimeCountBefore() > 0 ? createCompleteFeatureTable(predecessorSelects, interval, intRange, totalBoundsStep) : null;
		QueryStep outcomeTable = form.getTimeCountAfter() > 0 ? createCompleteOutcomeTable(predecessorSelects, interval, intRange, totalBoundsStep) : null;

		return QueryStep.createUnionAllStep(
				Stream.concat(Stream.ofNullable(outcomeTable), Stream.ofNullable(featureTable)).toList(),
				FormCteStep.COMPLETE.getSuffix(),
				Collections.emptyList()
		);
	}

	private QueryStep createCompleteFeatureTable(Selects predecessorSelects, Interval interval, Range<Integer> intRange, QueryStep totalBoundsStep) {
		Field<Integer> featureIndex = DSL.field(DSL.val(-1)).as(SharedAliases.INDEX.getAlias());
		SqlIdColumns featureIds = predecessorSelects.getIds().withRelativeStratification(Resolution.COMPLETE, featureIndex, INDEX_SELECTOR);
		Field<Date> rangeStart = stratificationFunctions.shiftByInterval(INDEX_START_NEGATIVE, interval, DSL.val(intRange.getMin()), Offset.NONE);
		return createIntervalStep(featureIds, rangeStart, INDEX_START_NEGATIVE, Optional.empty(), totalBoundsStep);
	}

	private QueryStep createCompleteOutcomeTable(Selects predecessorSelects, Interval interval, Range<Integer> intRange, QueryStep totalBoundsStep) {
		Field<Integer> outcomeIndex = DSL.field(DSL.val(1)).as(SharedAliases.INDEX.getAlias());
		SqlIdColumns outcomeIds = predecessorSelects.getIds().withRelativeStratification(Resolution.COMPLETE, outcomeIndex, INDEX_SELECTOR);
		Field<Date> rangeEnd = stratificationFunctions.shiftByInterval(INDEX_START_POSITIVE, interval, DSL.val(intRange.getMax()), Offset.NONE);
		return createIntervalStep(outcomeIds, INDEX_START_POSITIVE, rangeEnd, Optional.empty(), totalBoundsStep);
	}

	private QueryStep createIntervalTable(QueryStep totalBoundsStep, Resolution resolution, RelativeFormQuery form) {

		Field<Integer> seriesIndex = stratificationFunctions.intSeriesField();
		Selects predecessorSelects = totalBoundsStep.getQualifiedSelects();
		SqlIdColumns ids = predecessorSelects.getIds().withRelativeStratification(resolution, seriesIndex, INDEX_SELECTOR);
		Interval interval = getInterval(form.getTimeUnit(), resolution);
		Range<Integer> bounds = toGenerateSeriesBounds(form, resolution);

		QueryStep timeBeforeStep = createFeatureTable(totalBoundsStep, interval, seriesIndex, bounds, ids);
		QueryStep timeAfterStep = createOutcomeTable(totalBoundsStep, interval, seriesIndex, bounds, ids);

		return QueryStep.createUnionAllStep(
				List.of(timeBeforeStep, timeAfterStep),
				FormCteStep.stratificationCte(resolution).getSuffix(),
				Collections.emptyList()
		);
	}

	private QueryStep createOutcomeTable(QueryStep totalBoundsStep, Interval interval, Field<Integer> seriesIndex, Range<Integer> bounds, SqlIdColumns ids) {
		Field<Date> outcomeRangeStart = stratificationFunctions.shiftByInterval(INDEX_START_POSITIVE, interval, seriesIndex, Offset.MINUS_ONE);
		Field<Date> outcomeRangeEnd = stratificationFunctions.shiftByInterval(INDEX_START_POSITIVE, interval, seriesIndex, Offset.NONE);
		Table<? extends Record> outcomeSeries = stratificationFunctions.generateIntSeries(1, bounds.getMax()).as(SharedAliases.INDEX.getAlias());
		return createIntervalStep(ids, outcomeRangeStart, outcomeRangeEnd, Optional.of(outcomeSeries), totalBoundsStep);
	}

	private QueryStep createFeatureTable(QueryStep totalBoundsStep, Interval interval, Field<Integer> seriesIndex, Range<Integer> bounds, SqlIdColumns ids) {
		Field<Date> featureRangeStart = stratificationFunctions.shiftByInterval(INDEX_START_NEGATIVE, interval, seriesIndex, Offset.NONE);
		Field<Date> featureRangeEnd = stratificationFunctions.shiftByInterval(INDEX_START_NEGATIVE, interval, seriesIndex, Offset.ONE);
		Table<? extends Record> featureSeries = stratificationFunctions.generateIntSeries(bounds.getMin(), -1).as(SharedAliases.INDEX.getAlias());
		return createIntervalStep(ids, featureRangeStart, featureRangeEnd, Optional.of(featureSeries), totalBoundsStep);
	}

	private QueryStep createIntervalStep(
			SqlIdColumns ids,
			Field<Date> rangeStart,
			Field<Date> rangeEnd,
			Optional<Table<? extends Record>> seriesTable,
			QueryStep predecessor
	) {
		Preconditions.checkArgument(
				predecessor.getSelects().getStratificationDate().isPresent(),
				"Expecting %s to contain a stratification date representing the min and max stratification bounds"
		);
		ColumnDateRange finalRange = functionProvider.intersection(
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
	 * Adjusts the {@link RelativeFormQuery#getTimeCountBefore()} && {@link RelativeFormQuery#getTimeCountAfter()} bounds, so they fit the SQL approach.
	 * Take time unit QUARTERS and Resolution YEARS as an example: If the time counts are not divisible by 4 (because 1 year == 4 quarters), we need to round
	 * up for each starting year. 5 Quarters mean 2 years we have to consider when creating the stratification.
	 */
	private static Range<Integer> toGenerateSeriesBounds(RelativeFormQuery relativeForm, Resolution resolution) {

		int timeCountBefore;
		int timeCountAfter;

		switch (relativeForm.getTimeUnit()) {
			case QUARTERS -> {
				if (resolution == Resolution.YEARS) {
					timeCountBefore = divideAndRoundUp(relativeForm.getTimeCountBefore(), 4);
					timeCountAfter = divideAndRoundUp(relativeForm.getTimeCountAfter(), 4);
				}
				else {
					timeCountBefore = relativeForm.getTimeCountBefore();
					timeCountAfter = relativeForm.getTimeCountAfter();
				}
			}
			case DAYS -> {
				switch (resolution) {
					case COMPLETE, DAYS -> {
						timeCountBefore = relativeForm.getTimeCountBefore();
						timeCountAfter = relativeForm.getTimeCountAfter();
					}
					case YEARS -> {
						timeCountBefore = divideAndRoundUp(relativeForm.getTimeCountBefore(), Interval.YEAR_AS_DAYS_INTERVAL.getAmount());
						timeCountAfter = divideAndRoundUp(relativeForm.getTimeCountAfter(), Interval.YEAR_AS_DAYS_INTERVAL.getAmount());
					}
					case QUARTERS -> {
						timeCountBefore = divideAndRoundUp(relativeForm.getTimeCountBefore(), Interval.NINETY_DAYS_INTERVAL.getAmount());
						timeCountAfter = divideAndRoundUp(relativeForm.getTimeCountAfter(), Interval.NINETY_DAYS_INTERVAL.getAmount());
					}
					default -> throw new CombinationNotSupportedException(relativeForm.getTimeUnit(), resolution);
				}
			}
			default -> throw new CombinationNotSupportedException(relativeForm.getTimeUnit(), resolution);
		}

		return Range.of(
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
	private static Interval getInterval(CalendarUnit timeUnit, Resolution resolution) {
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

}

package com.bakdata.conquery.sql.compiler.forms;

import static org.jooq.impl.DSL.*;

import java.sql.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.SharedAliases;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.model.form.FormResolution;
import com.bakdata.conquery.sql.model.form.ResolutionAndAlignment;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;

@RequiredArgsConstructor
class AbsoluteStratification {

	private final int INDEX_START = 1;
	private final int INDEX_END = 10_000;

	private final QueryStep baseStep;
	private final StratificationFunctions stratificationFunctions;

	public QueryStep createStratificationTable(List<ResolutionAndAlignment> resolutionAndAlignments, boolean negate) {

		QueryStep intSeriesStep = createIntSeriesStep();
		QueryStep indexStartStep = createIndexStartStep();

		List<QueryStep> resolutionTables = resolutionAndAlignments.stream()
														.map(resolutionAndAlignment -> createResolutionTable(indexStartStep, resolutionAndAlignment))
														.toList();

		List<QueryStep> predecessors = List.of(baseStep, intSeriesStep, indexStartStep);
		return StratificationTableFactory.unionResolutionTables(resolutionTables, predecessors, negate);
	}

	private QueryStep createIntSeriesStep() {

		// not actually required, but Selects expect at least 1 SqlIdColumn
		Field<String> rowNumber = rowNumber().over().coerce(String.class);
		SqlIdColumns ids = new SqlIdColumns(rowNumber);

		FieldWrapper<Integer> seriesIndex = new FieldWrapper<>(stratificationFunctions.intSeriesField());

		Selects selects = Selects.builder()
								 .ids(ids)
								 .sqlSelect(seriesIndex)
								 .build();

		Table<Record> seriesTable = stratificationFunctions.generateIntSeries(INDEX_START, INDEX_END).asTable(SharedAliases.INDEX.getAlias());

		return QueryStep.builder()
						.cteName(FormCteStep.INT_SERIES.getSuffix())
						.selects(selects)
						.fromTable(seriesTable)
						.build();
	}

	private QueryStep createIndexStartStep() {

		Selects baseStepSelects = baseStep.getQualifiedSelects();
		if (baseStepSelects.getStratificationDate().isEmpty()) {
			throw new IllegalArgumentException("The base step must have a stratification date set");
		}
		ColumnDateRange bounds = baseStepSelects.getStratificationDate().get();

		Field<Date> indexStart = stratificationFunctions.absoluteIndexStartDate(bounds).as(SharedAliases.INDEX_START.getAlias());
		Field<Date> yearStart = stratificationFunctions.lowerBoundYearStart(bounds).as(SharedAliases.YEAR_START.getAlias());
		Field<Date> yearEnd = stratificationFunctions.upperBoundYearEnd(bounds).as(SharedAliases.YEAR_END.getAlias());
		Field<Date> yearEndQuarterAligned = stratificationFunctions.upperBoundYearEndQuarterAligned(bounds).as(SharedAliases.YEAR_END_QUARTER_ALIGNED.getAlias());
		Field<Date> quarterStart = stratificationFunctions.lowerBoundQuarterStart(bounds).as(SharedAliases.QUARTER_START.getAlias());
		Field<Date> quarterEnd = stratificationFunctions.upperBoundQuarterEnd(bounds).as(SharedAliases.QUARTER_END.getAlias());

		List<FieldWrapper<Date>> startDates = Stream.of(
															indexStart,
															yearStart,
															yearEnd,
															yearEndQuarterAligned,
															quarterStart,
															quarterEnd
													)
													.map(FieldWrapper::new)
													.toList();

		Selects selects = Selects.builder()
								 .ids(baseStepSelects.getIds())
								 .stratificationDate(Optional.of(bounds))
								 .sqlSelects(startDates)
								 .build();

		return QueryStep.builder()
						.cteName(FormCteStep.INDEX_START.getSuffix())
						.selects(selects)
						.fromTable(QueryStep.toTableLike(baseStep.getCteName()))
						.build();
	}

	private QueryStep createResolutionTable(QueryStep indexStartStep, ResolutionAndAlignment resolutionAndAlignment) {
		return switch (resolutionAndAlignment.getResolution()) {
			case COMPLETE -> createCompleteTable();
			case YEARS, QUARTERS, DAYS -> createIntervalTable(indexStartStep, resolutionAndAlignment);
		};
	}

	private QueryStep createCompleteTable() {

		Selects baseStepSelects = baseStep.getQualifiedSelects();

		// complete range shall have a null index because it spans the complete range, but we set it to 1 to ensure we can join tables on index,
		// because a condition involving null in a join (e.g., null = some_value or null = null) always evaluates to false
		Field<Integer> index = field(inline(1)).as(SharedAliases.INDEX.getAlias());
		SqlIdColumns ids = baseStepSelects.getIds().withStratification(FormResolution.COMPLETE.name(), index);

		ColumnDateRange completeRange = baseStepSelects.getStratificationDate().get();

		Selects selects = Selects.builder()
								 .ids(ids)
								 .stratificationDate(Optional.of(completeRange))
								 .build();

		return QueryStep.builder()
						.cteName(FormCteStep.COMPLETE.getSuffix())
						.selects(selects)
						.fromTable(QueryStep.toTableLike(baseStep.getCteName()))
						.build();
	}

	private QueryStep createIntervalTable(QueryStep indexStartStep, ResolutionAndAlignment resolutionAndAlignment) {

		QueryStep countsCte = createCountsCte(indexStartStep, resolutionAndAlignment);
		if (countsCte.getSelects().getStratificationDate().isEmpty()) {
			throw new IllegalArgumentException("The countsCte must have a stratification date set");
		}
		Selects countsCteSelects = countsCte.getQualifiedSelects();

		ColumnDateRange stratificationRange = stratificationFunctions.createStratificationRange(
				resolutionAndAlignment,
				countsCteSelects.getStratificationDate().get()
		);

		Field<Integer> index = stratificationFunctions.index(countsCteSelects.getIds(), countsCte.getQualifiedSelects().getStratificationDate());
		SqlIdColumns ids = countsCteSelects.getIds().withStratification(resolutionAndAlignment.getResolution().name(), index);

		Selects selects = Selects.builder()
								 .ids(ids)
								 .stratificationDate(Optional.ofNullable(stratificationRange))
								 .build();

		Condition stopOnMaxResolutionWindowCount = stratificationFunctions.stopOnMaxResolutionWindowCount(resolutionAndAlignment);

		return QueryStep.builder()
						.cteName(FormCteStep.stratificationCte(resolutionAndAlignment.getResolution()).getSuffix())
						.selects(selects)
						.fromTable(QueryStep.toTableLike(countsCte.getCteName()))
						.fromTable(QueryStep.toTableLike(FormCteStep.INT_SERIES.getSuffix()))
						.conditions(List.of(stopOnMaxResolutionWindowCount))
						.predecessor(countsCte)
						.build();
	}

	private QueryStep createCountsCte(QueryStep indexStartStep, ResolutionAndAlignment resolutionAndAlignment) {

		Selects indexStartSelects = indexStartStep.getQualifiedSelects();
		if (indexStartSelects.getStratificationDate().isEmpty()) {
			throw new IllegalArgumentException("The indexStartStep must have a stratification date set");
		}

		Field<Integer> resolutionWindowCount = stratificationFunctions.calculateResolutionWindowCount(
				resolutionAndAlignment,
				indexStartSelects.getStratificationDate().get()
		);

		Selects selects = indexStartSelects.toBuilder()
										   .sqlSelect(new FieldWrapper<>(resolutionWindowCount))
										   .build();

		return QueryStep.builder()
						.cteName(FormCteStep.countsCte(resolutionAndAlignment.getResolution()).getSuffix())
						.selects(selects)
						.fromTable(QueryStep.toTableLike(indexStartStep.getCteName()))
						.build();
	}

}

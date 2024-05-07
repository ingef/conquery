package com.bakdata.conquery.sql.conversion.forms;

import java.sql.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

@Getter(AccessLevel.PROTECTED)
@RequiredArgsConstructor
public class StratificationTableFactory {

	private final int INDEX_START = 1;
	private final int INDEX_END = 10_000;

	private final QueryStep baseStep;
	private final StratificationFunctions stratificationFunctions;
	private final SqlFunctionProvider functionProvider;

	public StratificationTableFactory(QueryStep baseStep, ConversionContext context) {
		this.baseStep = baseStep;
		this.stratificationFunctions = StratificationFunctions.create(context);
		this.functionProvider = context.getSqlDialect().getFunctionProvider();
	}

	public QueryStep createStratificationTable(List<ExportForm.ResolutionAndAlignment> resolutionAndAlignments) {

		QueryStep intSeriesStep = createIntSeriesStep();
		QueryStep indexStartStep = createIndexStartStep();

		List<QueryStep> tables = resolutionAndAlignments.stream()
														.map(resolutionAndAlignment -> createResolutionTable(indexStartStep, resolutionAndAlignment))
														.toList();

		List<QueryStep> predecessors = List.of(baseStep, intSeriesStep, indexStartStep);
		return unionResolutionTables(tables, predecessors);
	}

	private QueryStep createIntSeriesStep() {

		// not actually required, but Selects expect at least 1 SqlIdColumn
		Field<Object> rowNumber = DSL.rowNumber().over().coerce(Object.class);
		SqlIdColumns ids = new SqlIdColumns(rowNumber);

		FieldWrapper<Integer> seriesIndex = new FieldWrapper<>(stratificationFunctions.intSeriesField());

		Selects selects = Selects.builder()
								 .ids(ids)
								 .sqlSelect(seriesIndex)
								 .build();

		Table<Record> seriesTable = stratificationFunctions.generateIntSeries(INDEX_START, INDEX_END)
														   .as(SharedAliases.SERIES_INDEX.getAlias());

		return QueryStep.builder()
						.cteName(FormCteStep.INT_SERIES.getSuffix())
						.selects(selects)
						.fromTable(seriesTable)
						.build();
	}

	private QueryStep createIndexStartStep() {

		Selects baseStepSelects = baseStep.getQualifiedSelects();
		Preconditions.checkArgument(baseStepSelects.getStratificationDate().isPresent(), "The base step must have a stratification date set");
		ColumnDateRange bounds = baseStepSelects.getStratificationDate().get();

		Field<Date> indexStart = stratificationFunctions.absoluteIndexStartDate(bounds).as(SharedAliases.INDEX_START.getAlias());
		Field<Date> yearStart = stratificationFunctions.yearStart(bounds).as(SharedAliases.YEAR_START.getAlias());
		Field<Date> yearEnd = stratificationFunctions.yearEnd(bounds).as(SharedAliases.YEAR_END.getAlias());
		Field<Date> yearEndQuarterAligned = stratificationFunctions.yearEndQuarterAligned(bounds).as(SharedAliases.YEAR_END_QUARTER_ALIGNED.getAlias());
		Field<Date> quarterStart = stratificationFunctions.quarterStart(bounds).as(SharedAliases.QUARTER_START.getAlias());
		Field<Date> quarterEnd = stratificationFunctions.quarterEnd(bounds).as(SharedAliases.QUARTER_END.getAlias());

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

	private QueryStep createResolutionTable(QueryStep indexStartStep, ExportForm.ResolutionAndAlignment resolutionAndAlignment) {
		return switch (resolutionAndAlignment.getResolution()) {
			case COMPLETE -> createCompleteTable();
			case YEARS, QUARTERS, DAYS -> createIntervalTable(indexStartStep, resolutionAndAlignment);
		};
	}

	private QueryStep createCompleteTable() {

		Selects baseStepSelects = baseStep.getQualifiedSelects();

		// complete range shall have a null index because it spans the complete range, but we set it to 1 to ensure we can join tables on index,
		// because a condition involving null in a join (e.g., null = some_value or null = null) always evaluates to false
		Field<Integer> index = DSL.val(1, Integer.class).as(SharedAliases.INDEX.getAlias());
		SqlIdColumns ids = baseStepSelects.getIds().withAbsoluteStratification(Resolution.COMPLETE, index);

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

	private QueryStep createIntervalTable(QueryStep indexStartStep, ExportForm.ResolutionAndAlignment resolutionAndAlignment) {

		QueryStep countsCte = createCountsCte(indexStartStep, resolutionAndAlignment);
		Preconditions.checkArgument(countsCte.getSelects().getStratificationDate().isPresent(), "The countsCte must have a stratification date set");
		Selects countsCteSelects = countsCte.getQualifiedSelects();

		ColumnDateRange stratificationRange = stratificationFunctions.createStratificationRange(
				resolutionAndAlignment,
				countsCteSelects.getStratificationDate().get()
		);

		Field<Integer> index = stratificationFunctions.index(countsCteSelects.getIds(), countsCte.getQualifiedSelects().getStratificationDate());
		SqlIdColumns ids = countsCteSelects.getIds().withAbsoluteStratification(resolutionAndAlignment.getResolution(), index);

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

	private QueryStep createCountsCte(QueryStep indexStartStep, ExportForm.ResolutionAndAlignment resolutionAndAlignment) {

		Selects indexStartSelects = indexStartStep.getQualifiedSelects();
		Preconditions.checkArgument(indexStartSelects.getStratificationDate().isPresent(), "The indexStartStep must have a stratification date set");

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

	private QueryStep unionResolutionTables(List<QueryStep> unionSteps, List<QueryStep> predecessors) {

		Preconditions.checkArgument(!unionSteps.isEmpty(), "Expecting at least 1 resolution table");

		List<QueryStep> withQualifiedSelects = unionSteps.stream()
														 .map(queryStep -> QueryStep.builder()
																					.selects(queryStep.getQualifiedSelects())
																					.fromTable(QueryStep.toTableLike(queryStep.getCteName()))
																					.build())
														 .toList();

		return QueryStep.createUnionStep(
				withQualifiedSelects,
				FormCteStep.FULL_STRATIFICATION.getSuffix(),
				Stream.concat(predecessors.stream(), unionSteps.stream()).toList()
		);
	}

}

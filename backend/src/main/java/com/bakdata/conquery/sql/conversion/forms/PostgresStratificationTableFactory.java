package com.bakdata.conquery.sql.conversion.forms;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.PostgreSqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

class PostgresStratificationTableFactory extends StratificationTableFactory implements AbsoluteStratification {

	public PostgresStratificationTableFactory(QueryStep baseStep, ConversionContext context) {
		super(baseStep, context);
	}

	@Override
	public QueryStep createIntervalTable(Range<LocalDate> formDateRestriction, ExportForm.ResolutionAndAlignment resolutionAndAlignment) {

		QueryStep seriesTableStep = createSeriesTableStep(resolutionAndAlignment, formDateRestriction);

		Selects baseStepSelects = getBaseStep().getQualifiedSelects();
		Field<Integer> index = indexField(baseStepSelects.getIds());
		SqlIdColumns ids = baseStepSelects.getIds().withAbsoluteStratification(resolutionAndAlignment.getResolution(), index);

		ColumnDateRange seriesRange = seriesTableStep.getQualifiedSelects().getStratificationDate().orElseThrow(
				() -> new IllegalStateException("Series table step should contain a stratification date")
		);
		ColumnDateRange bounds = findBounds(formDateRestriction, baseStepSelects);
		ColumnDateRange stratificationDate = createStratificationDateRange(seriesRange, bounds);

		Selects selects = Selects.builder()
								 .ids(ids)
								 .stratificationDate(Optional.ofNullable(stratificationDate))
								 .build();

		List<Condition> conditions = stratificationTableConditions(seriesRange, bounds);
		List<TableLike<Record>> tables = List.of(
				QueryStep.toTableLike(getBaseStep().getCteName()),
				QueryStep.toTableLike(seriesTableStep.getCteName())
		);

		return QueryStep.builder()
						.cteName(FormCteStep.stratificationCte(resolutionAndAlignment.getResolution()).getSuffix())
						.selects(selects)
						.fromTables(tables)
						.conditions(conditions)
						.predecessors(List.of(seriesTableStep))
						.build();
	}

	@Override
	protected PostgreSqlFunctionProvider getFunctionProvider() {
		return (PostgreSqlFunctionProvider) super.getFunctionProvider();
	}

	/**
	 * Unlike HANA, Postgres does not create a start and end date when creating a date series. Instead, it just defines timestamps from start to end in a
	 * single row set. That's why we have to define start and end ourselves via SQL's lead() function.
	 */
	private QueryStep createSeriesTableStep(ExportForm.ResolutionAndAlignment resolutionAndAlignment, Range<LocalDate> dateRange) {

		Table<Record> seriesTable = createSeries(dateRange, resolutionAndAlignment);

		// series are generated as timestamps, so we have to cast
		Field<Date> seriesField = getFunctionProvider().cast(DSL.field(DSL.name(SharedAliases.DATE_SERIES.getAlias()), Timestamp.class), SQLDataType.DATE);
		Field<Date> startDate = seriesField.as(SharedAliases.DATE_START.getAlias());
		Field<Date> endDate = DSL.lead(seriesField).over().as(SharedAliases.DATE_END.getAlias());
		ColumnDateRange seriesRange = ColumnDateRange.of(startDate, endDate);

		// not actually required, but Selects expect at least 1 SqlIdColumn
		Field<Object> rowNumber = DSL.rowNumber().over().coerce(Object.class);
		SqlIdColumns ids = new SqlIdColumns(rowNumber);

		Selects selects = Selects.builder()
								 .ids(ids)
								 .stratificationDate(Optional.of(seriesRange))
								 .build();

		return QueryStep.builder()
						.cteName(FormCteStep.seriesCte(resolutionAndAlignment.getResolution()).getSuffix())
						.selects(selects)
						.fromTable(seriesTable)
						.build();
	}

	private Table<Record> createSeries(Range<LocalDate> dateRange, ExportForm.ResolutionAndAlignment resolutionAndAlignment) {

		PostgreSqlFunctionProvider functionProvider = getFunctionProvider();
		Range<LocalDate> adjustedRange = toGenerateSeriesBounds(dateRange, resolutionAndAlignment);
		Field<Timestamp> start = functionProvider.cast(
				DSL.field(DSL.val(adjustedRange.getMin().toString())),
				SQLDataType.TIMESTAMP
		);
		Field<Timestamp> end = functionProvider.cast(
				DSL.field(DSL.val(adjustedRange.getMax().toString())),
				SQLDataType.TIMESTAMP
		);

		return DSL.table(
						  "generate_series({0}, {1}, {2} {3})",
						  start,
						  end,
						  DSL.keyword("interval"),
						  DSL.val(toResolutionExpression(resolutionAndAlignment.getResolution()))
				  )
				  .as(SharedAliases.DATE_SERIES.getAlias());
	}

	@Override
	public ColumnDateRange findBounds(Range<LocalDate> formDateRestriction, Selects baseStepSelects) {
		ColumnDateRange bounds;
		if (isEntityDateStratification()) {
			bounds = getFunctionProvider().toDualColumn(baseStepSelects.getValidityDate().get());
		}
		else {
			Field<Date> formDateRangeMin = getFunctionProvider().toDateField(formDateRestriction.getMin().toString());
			Field<Date> formDateRangeMax = getFunctionProvider().addDays(getFunctionProvider().toDateField(formDateRestriction.getMax().toString()), 1);
			bounds = ColumnDateRange.of(formDateRangeMin, formDateRangeMax);
		}
		return bounds;
	}

	@Override
	public ColumnDateRange createStratificationDateRange(ColumnDateRange seriesRange, ColumnDateRange bounds) {
		PostgreSqlFunctionProvider functionProvider = getFunctionProvider();
		Field<Date> rangeStart = DSL.greatest(seriesRange.getStart(), bounds.getStart());
		Field<Date> rangeEnd = DSL.least(seriesRange.getEnd(), bounds.getEnd());
		Field<?> daterange = functionProvider.daterange(rangeStart, rangeEnd, "[)");
		return ColumnDateRange.of(daterange).as(SharedAliases.STRATIFICATION_RANGE.getAlias());
	}

	@Override
	public List<Condition> stratificationTableConditions(ColumnDateRange seriesRange, ColumnDateRange bounds) {

		PostgreSqlFunctionProvider functionProvider = getFunctionProvider();

		// we need to filter the single entry with a null end date which is created because we use the SQL lead() function in createSeriesTableStep()
		Field<Object> stratificationDateEnd = DSL.field(DSL.name(SharedAliases.DATE_END.getAlias()));
		Condition endNotNull = DSL.condition(stratificationDateEnd.isNotNull());

		Condition overlapCondition = isEntityDateStratification() ? functionProvider.dateRestriction(bounds, seriesRange) : DSL.noCondition();

		return List.of(endNotNull, overlapCondition);
	}

}

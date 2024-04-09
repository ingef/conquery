package com.bakdata.conquery.sql.conversion.forms;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import lombok.Getter;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

@Getter
class HanaStratificationTableFactory extends StratificationTableFactory implements AbsoluteStratification {

	// HANA pre-generates start and end date when generating a date series
	// see https://help.sap.com/docs/SAP_HANA_PLATFORM/4fe29514fd584807ac9f2a04f6754767/c8101037ad4344768db31e68e4d30eb4.html
	private static final Field<Date> SERIES_START_FIELD = DSL.field(DSL.name("GENERATED_PERIOD_START"), Date.class);
	private static final Field<Date> SERIES_END_FIELD = DSL.field(DSL.name("GENERATED_PERIOD_END"), Date.class);

	public HanaStratificationTableFactory(QueryStep baseStep, ConversionContext context) {
		super(baseStep, context);
	}

	@Override
	public QueryStep createIntervalTable(Range<LocalDate> formDateRestriction, ExportForm.ResolutionAndAlignment resolutionAndAlignment) {

		Table<Record> seriesTable = createGenerateSeriesTable(formDateRestriction, resolutionAndAlignment);

		Selects baseStepSelects = getBaseStep().getQualifiedSelects();
		SqlIdColumns ids = baseStepSelects.getIds().withAbsoluteStratification(resolutionAndAlignment.getResolution(), indexField(baseStepSelects.getIds()));

		ColumnDateRange bounds = findBounds(formDateRestriction, baseStepSelects);
		ColumnDateRange seriesRange = ColumnDateRange.of(SERIES_START_FIELD, SERIES_END_FIELD);
		ColumnDateRange yearRange = createStratificationDateRange(seriesRange, bounds);

		Selects selects = Selects.builder()
								 .ids(ids)
								 .stratificationDate(Optional.ofNullable(yearRange))
								 .build();

		List<Condition> conditions = stratificationTableConditions(seriesRange, bounds);
		List<TableLike<Record>> tables = List.of(QueryStep.toTableLike(getBaseStep().getCteName()), seriesTable);

		return QueryStep.builder()
						.cteName(FormCteStep.stratificationCte(resolutionAndAlignment.getResolution()).getSuffix())
						.selects(selects)
						.fromTables(tables)
						.conditions(conditions)
						.build();
	}

	@Override
	public ColumnDateRange createStratificationDateRange(ColumnDateRange seriesRange, ColumnDateRange bounds) {
		Field<Date> rangeStart = DSL.greatest(seriesRange.getStart(), bounds.getStart());
		Field<Date> rangeEnd = DSL.least(seriesRange.getEnd(), bounds.getEnd());
		return ColumnDateRange.of(rangeStart, rangeEnd).as(SharedAliases.STRATIFICATION_RANGE.getAlias());
	}

	@Override
	public ColumnDateRange findBounds(Range<LocalDate> formDateRestriction, Selects baseStepSelects) {
		ColumnDateRange bounds;
		if (isEntityDateStratification()) {
			bounds = getFunctionProvider().toDualColumn(baseStepSelects.getValidityDate().get());
		}
		else {
			bounds = getFunctionProvider().forCDateRange(CDateRange.of(formDateRestriction));
		}
		return bounds;
	}

	@Override
	public List<Condition> stratificationTableConditions(ColumnDateRange seriesRange, ColumnDateRange bounds) {
		if (!isEntityDateStratification()) {
			return Collections.emptyList();
		}
		return List.of(getFunctionProvider().dateRestriction(bounds, seriesRange));
	}

	private Table<Record> createGenerateSeriesTable(Range<LocalDate> formDateRestriction, ExportForm.ResolutionAndAlignment resolutionAndAlignment) {
		// upper-casing not required, but HANA best-practice
		String resolutionExpression = toResolutionExpression(resolutionAndAlignment.getResolution());
		Range<LocalDate> adjustedRange = toGenerateSeriesBounds(formDateRestriction, resolutionAndAlignment);
		Field<Date> start = getFunctionProvider().toDateField(adjustedRange.getMin().toString());
		Field<Date> end = getFunctionProvider().toDateField(adjustedRange.getMax().toString());
		return DSL.table("SERIES_GENERATE_DATE({0}, {1}, {2})", DSL.val("INTERVAL %s".formatted(resolutionExpression)), start, end);
	}

}

package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.DateDistanceFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DateDistanceSelect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.DateDistanceCondition;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import lombok.Value;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Value
public class DateDistanceSqlAggregator implements SqlAggregator {

	SqlSelects sqlSelects;
	WhereClauses whereClauses;

	public DateDistanceSqlAggregator(
			Column column,
			String alias,
			ChronoUnit timeUnit,
			SqlTables tables,
			Range.LongRange filterValue,
			ConversionContext conversionContext
	) {
		if (column.getType() != MajorTypeId.DATE) {
			throw new UnsupportedOperationException("Can't calculate date distance to column of type " + column.getType());
		}

		Field<Date> startDate = DSL.field(DSL.name(tables.getRootTable(), column.getName()), Date.class);
		Field<Date> endDate = getEndDate(conversionContext);

		SqlFunctionProvider functionProvider = conversionContext.getSqlDialect().getFunctionProvider();
		FieldWrapper<Integer> dateDistanceSelect = new FieldWrapper<>(functionProvider.dateDistance(timeUnit, startDate, endDate).as(alias));

		SqlSelects.SqlSelectsBuilder builder = SqlSelects.builder().preprocessingSelect(dateDistanceSelect);

		if (filterValue == null) {
			Field<Integer> qualifiedDateDistance = dateDistanceSelect.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT))
																	 .select();
			FieldWrapper<Integer> minDateDistance = new FieldWrapper<>(DSL.min(qualifiedDateDistance).as(alias));

			String finalPredecessor = tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
			ExtractingSqlSelect<Integer> finalSelect = minDateDistance.qualify(finalPredecessor);

			this.sqlSelects = builder.aggregationSelect(minDateDistance)
									 .finalSelect(finalSelect)
									 .build();
			this.whereClauses = WhereClauses.empty();
		}
		else {
			this.sqlSelects = builder.build();
			String predecessorCte = tables.getPredecessor(ConceptCteStep.EVENT_FILTER);
			Field<Integer> qualifiedDateDistanceSelect = dateDistanceSelect.qualify(predecessorCte).select();
			WhereCondition dateDistanceCondition = new DateDistanceCondition(qualifiedDateDistanceSelect, filterValue);
			this.whereClauses = WhereClauses.builder()
											.eventFilter(dateDistanceCondition)
											.build();
		}
	}

	public static DateDistanceSqlAggregator create(
			DateDistanceSelect dateDistanceSelect,
			SelectContext selectContext
	) {
		return new DateDistanceSqlAggregator(
				dateDistanceSelect.getColumn().resolve(),
				selectContext.getNameGenerator().selectName(dateDistanceSelect),
				dateDistanceSelect.getTimeUnit(),
				selectContext.getTables(),
				null,
				selectContext.getConversionContext()
		);
	}

	public static DateDistanceSqlAggregator create(
			DateDistanceFilter dateDistanceFilter,
			FilterContext<Range.LongRange> filterContext
	) {
		return new DateDistanceSqlAggregator(
				dateDistanceFilter.getColumn().resolve(),
				filterContext.getNameGenerator().selectName(dateDistanceFilter),
				dateDistanceFilter.getTimeUnit(),
				filterContext.getTables(),
				filterContext.getValue(),
				filterContext.getConversionContext()
		);
	}

	private Field<Date> getEndDate(ConversionContext conversionContext) {

		SqlFunctionProvider functionProvider = conversionContext.getSqlDialect().getFunctionProvider();

		// if there is a stratification active, the upper bound of the stratification date is the end date
		if (conversionContext.isWithStratification()) {
			ColumnDateRange stratificationDate = conversionContext.getStratificationTable().getQualifiedSelects().getStratificationDate().get();
			ColumnDateRange dualColumn = functionProvider.toDualColumn(stratificationDate);
			// end date is allways treated exclusive, so we get the actual end date when subtracting 1 day
			return functionProvider.addDays(dualColumn.getEnd(), DSL.val(-1));
		}

		LocalDate endDate;
		// if a date restriction is set, the max of the date restriction equals the end date of the date distance
		// but there is also the possibility that the user set's an empty daterange which will be non-null but with null values
		CDateRange dateRestriction = conversionContext.getDateRestrictionRange();
		if (dateRestriction != null && dateRestriction.getMax() != null) {
			endDate = dateRestriction.getMax();
		}
		else {
			// otherwise the current date is the upper bound
			endDate = conversionContext.getSqlDialect().getDateNowSupplier().getLocalDateNow();
		}
		return functionProvider.toDateField(Date.valueOf(endDate).toString());
	}

}

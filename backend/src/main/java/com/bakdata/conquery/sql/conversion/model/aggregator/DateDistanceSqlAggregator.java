package com.bakdata.conquery.sql.conversion.model.aggregator;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.DateDistanceFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DateDistanceSelect;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.*;
import com.bakdata.conquery.sql.conversion.model.select.*;
import org.jooq.Condition;
import org.jooq.Field;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.DSL.field;

public class DateDistanceSqlAggregator implements SelectConverter<DateDistanceSelect>, FilterConverter<DateDistanceFilter, Range.LongRange> {

	@Override
	public ConnectorSqlSelects connectorSelect(DateDistanceSelect select, SelectContext<ConnectorSqlTables> selectContext) {

		Column column = select.getColumn().resolve();
		String alias = selectContext.getNameGenerator().selectName(select);
		ConnectorSqlTables tables = selectContext.getTables();

		Field<Integer> dateDistanceSelect = createDateDistanceSelect(column, select.getTimeUnit(), tables, selectContext.getConversionContext())
				.as(alias);
		FieldWrapper<Integer> dateDistanceWrapper = new FieldWrapper<>(dateDistanceSelect);

		Field<Integer> qualifiedDateDistance = dateDistanceWrapper.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT)).select();
		FieldWrapper<Integer> minDateDistance = new FieldWrapper<>(min(qualifiedDateDistance).as(alias));

		String finalPredecessor = tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
		ExtractingSqlSelect<Integer> finalSelect = minDateDistance.qualify(finalPredecessor);

		return ConnectorSqlSelects.builder()
								  .preprocessingSelect(dateDistanceWrapper)
								  .aggregationSelect(minDateDistance)
								  .finalSelect(finalSelect)
								  .build();
	}

	@Override
	public SqlFilters convertToSqlFilter(DateDistanceFilter filter, FilterContext<Range.LongRange> filterContext) {

		Column column = filter.getColumn().resolve();
		Field<Integer> dateDistance = createDateDistanceSelect(column , filter.getTimeUnit(), filterContext.getTables(), filterContext.getConversionContext());

		WhereCondition dateDistanceCondition = new DateDistanceCondition(dateDistance, filterContext.getValue());

		WhereClauses whereClauses = WhereClauses.builder().eventFilter(dateDistanceCondition).build();

		return new SqlFilters(ConnectorSqlSelects.none(), whereClauses);
	}

	@Override
	public Condition convertForTableExport(DateDistanceFilter filter, FilterContext<Range.LongRange> filterContext) {

		Column column = filter.getColumn().resolve();
		Field<Integer> dateDistance = createDateDistanceSelect(column , filter.getTimeUnit(), filterContext.getTables(), filterContext.getConversionContext());

		return new DateDistanceCondition(dateDistance, filterContext.getValue()).condition();
	}

	private Field<Integer> createDateDistanceSelect(
			Column column,
			ChronoUnit timeUnit,
			SqlTables tables,
			ConversionContext conversionContext
	) {
        Field<Date> startDate = field(name(tables.getRootTable(), column.getName()), Date.class);

        Field<Date> endDate = getEndDate(conversionContext);

		SqlFunctionProvider functionProvider = conversionContext.getFunctionProvider();
		return functionProvider.dateDistance(timeUnit, startDate, endDate);
	}

	private Field<Date> getEndDate(ConversionContext conversionContext) {

		SqlFunctionProvider functionProvider = conversionContext.getFunctionProvider();

		// if there is a stratification active, the upper bound of the stratification date is the end date
		if (conversionContext.isWithStratification()) {
			ColumnDateRange stratificationDate = conversionContext.getStratificationTable().getQualifiedSelects().getStratificationDate().get();
			ColumnDateRange dualColumn = functionProvider.toDualColumn(stratificationDate);
			// end date is allways treated exclusive, so we get the actual end date when subtracting 1 day
			return functionProvider.addDays(dualColumn.getEnd(), inline(-1));
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
			endDate = LocalDate.now(conversionContext.getClock());
		}
		return functionProvider.toDateField(Date.valueOf(endDate).toString());
	}


}

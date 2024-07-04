package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.DateDistanceFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DateDistanceSelect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.forms.StratificationFunctions;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.DateDistanceCondition;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class DateDistanceSqlAggregator implements SelectConverter<DateDistanceSelect>, FilterConverter<DateDistanceFilter, Range.LongRange> {

	@Override
	public ConnectorSqlSelects connectorSelect(DateDistanceSelect select, SelectContext<Connector, ConnectorSqlTables> selectContext) {

		Column column = select.getColumn();
		String alias = selectContext.getNameGenerator().selectName(select);
		ConnectorSqlTables tables = selectContext.getTables();
		ConversionContext conversionContext = selectContext.getConversionContext();

		FieldWrapper<Integer> dateDistanceSelect = createDateDistanceSelect(column, alias, select.getTimeUnit(), tables, conversionContext);

		Field<Integer> qualifiedDateDistance = dateDistanceSelect.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT)).select();
		FieldWrapper<Integer> minDateDistance = new FieldWrapper<>(DSL.min(qualifiedDateDistance).as(alias));

		String finalPredecessor = tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
		ExtractingSqlSelect<Integer> finalSelect = minDateDistance.qualify(finalPredecessor);

		return ConnectorSqlSelects.builder()
								  .preprocessingSelect(dateDistanceSelect)
								  .aggregationSelect(minDateDistance)
								  .finalSelect(finalSelect)
								  .build();
	}

	@Override
	public SqlFilters convertToSqlFilter(DateDistanceFilter filter, FilterContext<Range.LongRange> filterContext) {

		Column column = filter.getColumn();
		String alias = filterContext.getNameGenerator().selectName(filter);
		ConnectorSqlTables tables = filterContext.getTables();
		ConversionContext conversionContext = filterContext.getConversionContext();

		FieldWrapper<Integer> dateDistanceSelect = createDateDistanceSelect(column, alias, filter.getTimeUnit(), tables, conversionContext);
		ConnectorSqlSelects selects = ConnectorSqlSelects.builder().preprocessingSelect(dateDistanceSelect).build();

		String eventFilterCteName = tables.getPredecessor(ConceptCteStep.EVENT_FILTER);
		Field<Integer> qualifiedDateDistanceSelect = dateDistanceSelect.qualify(eventFilterCteName).select();
		WhereCondition dateDistanceCondition = new DateDistanceCondition(qualifiedDateDistanceSelect, filterContext.getValue());

		WhereClauses whereClauses = WhereClauses.builder().eventFilter(dateDistanceCondition).build();

		return new SqlFilters(selects, whereClauses);
	}

	@Override
	public Condition convertForTableExport(DateDistanceFilter filter, FilterContext<Range.LongRange> filterContext) {

		Column column = filter.getColumn();
		String tableName = column.getTable().getName();
		String columnName = column.getName();

		Field<Date> startDateField = DSL.field(DSL.name(tableName, columnName), Date.class);
		Field<Date> endDate = getEndDate(filterContext.getConversionContext());

		Field<Integer> dateDistance = filterContext.getFunctionProvider().dateDistance(filter.getTimeUnit(), startDateField, endDate);
		return new DateDistanceCondition(dateDistance, filterContext.getValue()).condition();
	}

	private FieldWrapper<Integer> createDateDistanceSelect(
			Column column,
			String alias,
			ChronoUnit timeUnit,
			SqlTables tables,
			ConversionContext conversionContext
	) {
		Field<Date> startDate;
		if (column.getType() == MajorTypeId.DATE) {
			startDate = DSL.field(DSL.name(tables.getRootTable(), column.getName()), Date.class);
		}
		else {
			StratificationFunctions stratificationFunctions = StratificationFunctions.create(conversionContext);
			Field<Date> daterangeColumn = DSL.field(DSL.name(tables.getRootTable(), column.getName()), Date.class);
			startDate = stratificationFunctions.lower(ColumnDateRange.of(daterangeColumn));
		}

		Field<Date> endDate = getEndDate(conversionContext);

		SqlFunctionProvider functionProvider = conversionContext.getSqlDialect().getFunctionProvider();
		return new FieldWrapper<>(functionProvider.dateDistance(timeUnit, startDate, endDate).as(alias));
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

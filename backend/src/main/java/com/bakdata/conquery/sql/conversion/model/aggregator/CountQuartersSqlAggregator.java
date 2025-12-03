package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CountQuartersFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.CountQuartersSelect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.dialect.Interval;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.forms.StratificationFunctions;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.filter.CountCondition;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Param;
import org.jooq.impl.DSL;

//TODO(FK): this needs a rework. Current implementation makes a sum of the quarters and doesn't take overlapping events into account.
public class CountQuartersSqlAggregator implements SelectConverter<CountQuartersSelect>, FilterConverter<CountQuartersFilter, Range.LongRange>, SqlAggregator {

	@Override
	public ConnectorSqlSelects connectorSelect(CountQuartersSelect countQuartersSelect, SelectContext<ConnectorSqlTables> selectContext) {

		String alias = selectContext.getNameGenerator().selectName(countQuartersSelect);
		ConnectorSqlTables tables = selectContext.getTables();
		SqlFunctionProvider functionProvider = selectContext.getFunctionProvider();
		StratificationFunctions stratificationFunctions = StratificationFunctions.create(selectContext.getConversionContext());

		CommonAggregationSelect<? extends Number> countAggregationSelect;
		if (countQuartersSelect.isSingleColumnDaterange()) {
			Column countColumn = countQuartersSelect.getColumn().resolve();
			countAggregationSelect = countColumn.getType() == MajorTypeId.DATE_RANGE
									 ? createSingleDaterangeColumnAggregationSelect(countColumn, alias, tables, functionProvider, stratificationFunctions)
									 : createSingleDateColumnAggregationSelect(countColumn, alias, tables, functionProvider);
		}
		else {
			Column startColumn = countQuartersSelect.getStartColumn().resolve();
			Column endColumn = countQuartersSelect.getEndColumn().resolve();
			countAggregationSelect = createTwoDateColumnAggregationSelect(startColumn, endColumn, alias, tables, functionProvider, stratificationFunctions);
		}

		String finalPredecessor = tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
		ExtractingSqlSelect<? extends Number> finalSelect = countAggregationSelect.getGroupBy().qualify(finalPredecessor);

		return ConnectorSqlSelects.builder()
								  .preprocessingSelects(countAggregationSelect.getRootSelects())
								  .aggregationSelect(countAggregationSelect.getGroupBy())
								  .finalSelect(finalSelect)
								  .build();
	}

	@Override
	public SqlFilters convertToSqlFilter(CountQuartersFilter countQuartersFilter, FilterContext<Range.LongRange> filterContext) {

		String alias = filterContext.getNameGenerator().selectName(countQuartersFilter);
		ConnectorSqlTables tables = filterContext.getTables();
		SqlFunctionProvider functionProvider = filterContext.getFunctionProvider();
		StratificationFunctions stratificationFunctions = StratificationFunctions.create(filterContext.getConversionContext());

		CommonAggregationSelect<? extends Number> countAggregationSelect;
		if (countQuartersFilter.isSingleColumnDaterange()) {
			Column countColumn = countQuartersFilter.getColumn().resolve();
			countAggregationSelect = countColumn.getType() == MajorTypeId.DATE_RANGE
									 ? createSingleDaterangeColumnAggregationSelect(countColumn, alias, tables, functionProvider, stratificationFunctions)
									 : createSingleDateColumnAggregationSelect(countColumn, alias, tables, functionProvider);
		}
		else {
			Column startColumn = countQuartersFilter.getStartColumn().resolve();
			Column endColumn = countQuartersFilter.getEndColumn().resolve();
			countAggregationSelect = createTwoDateColumnAggregationSelect(startColumn, endColumn, alias, tables, functionProvider, stratificationFunctions);
		}
		ConnectorSqlSelects selects = ConnectorSqlSelects.builder()
														 .preprocessingSelects(countAggregationSelect.getRootSelects())
														 .aggregationSelect(countAggregationSelect.getGroupBy())
														 .build();

		String predecessorTableName = tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
		Field<? extends Number> qualifiedCountSelect = countAggregationSelect.getGroupBy().qualify(predecessorTableName).select();
		CountCondition countCondition = new CountCondition(qualifiedCountSelect, filterContext.getValue());
		WhereClauses whereClauses = WhereClauses.builder()
												.groupFilter(countCondition)
												.build();

		return new SqlFilters(selects, whereClauses);
	}

	@Override
	public Condition convertForTableExport(CountQuartersFilter filter, FilterContext<Range.LongRange> filterContext) {
		Param<Integer> field = DSL.val(1); // no grouping, count is always 1 per row
		return new CountCondition(field, filterContext.getValue()).condition();
	}

	private static CommonAggregationSelect<Integer> createSingleDateColumnAggregationSelect(
			Column countColumn,
			String alias,
			ConnectorSqlTables tables,
			SqlFunctionProvider functionProvider
	) {
		ExtractingSqlSelect<Date> rootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), countColumn.getName(), Date.class);

		Field<Date> qualifiedRootSelect = rootSelect.qualify(tables.cteName(ConceptCteStep.EVENT_FILTER)).select();
		FieldWrapper<Integer> countQuartersAggregation = new FieldWrapper<>(
				DSL.countDistinct(functionProvider.yearQuarter(qualifiedRootSelect)).as(alias),
				countColumn.getName()
		);

		return CommonAggregationSelect.<Integer>builder()
									  .rootSelect(rootSelect)
									  .groupBy(countQuartersAggregation)
									  .build();
	}

	private static CommonAggregationSelect<BigDecimal> createSingleDaterangeColumnAggregationSelect(
			Column countColumn,
			String alias,
			ConnectorSqlTables tables,
			SqlFunctionProvider functionProvider,
			StratificationFunctions stratificationFunctions
	) {
		String rootTable = tables.getRootTable();
		ColumnDateRange daterange = ColumnDateRange.of(DSL.field(DSL.name(rootTable, countColumn.getName())));

		Field<Date> quarterStart = stratificationFunctions.lowerBoundQuarterStart(daterange);
		Field<Date> nextQuarterStart = stratificationFunctions.upperBoundQuarterEnd(daterange);

		return sumQuarterCount(quarterStart, nextQuarterStart, alias, tables, functionProvider);
	}

	private static CommonAggregationSelect<BigDecimal> createTwoDateColumnAggregationSelect(
			Column startColumn,
			Column endColumn,
			String alias,
			ConnectorSqlTables tables,
			SqlFunctionProvider functionProvider,
			StratificationFunctions stratificationFunctions
	) {
		String rootTable = tables.getRootTable();
		Field<Date> startDate = DSL.field(DSL.name(rootTable, startColumn.getName()), Date.class);
		Field<Date> endDate = DSL.field(DSL.name(rootTable, endColumn.getName()), Date.class);

		Field<Date> quarterStart = stratificationFunctions.jumpToQuarterStart(startDate);
		Field<Date> nextQuarterStart = stratificationFunctions.jumpToNextQuarterStart(endDate);
		return sumQuarterCount(quarterStart, nextQuarterStart, alias, tables, functionProvider);
	}

	private static CommonAggregationSelect<BigDecimal> sumQuarterCount(
			Field<Date> quarterStart,
			Field<Date> nextQuarterStart,
			String alias,
			ConnectorSqlTables tables,
			SqlFunctionProvider functionProvider
	) {
		Field<Integer> quarterCount = calcQuarterCount(quarterStart, nextQuarterStart, alias, functionProvider);
		FieldWrapper<Integer> quarterCountWrapper = new FieldWrapper<>(quarterCount);

		Field<Integer> qualifiedQuarterCount = quarterCountWrapper.qualify(tables.cteName(ConceptCteStep.EVENT_FILTER)).select();
		FieldWrapper<BigDecimal> quarterCountAggregation = new FieldWrapper<>(DSL.sum(qualifiedQuarterCount).as(alias));

		return CommonAggregationSelect.<BigDecimal>builder()
									  .rootSelect(quarterCountWrapper)
									  .groupBy(quarterCountAggregation)
									  .build();
	}

	private static Field<Integer> calcQuarterCount(Field<Date> quarterStart, Field<Date> nextQuarterStart, String alias, SqlFunctionProvider functionProvider) {
		return functionProvider.dateDistance(ChronoUnit.MONTHS, quarterStart, nextQuarterStart)
							   .divide(Interval.QUARTER_INTERVAL.getAmount())
							   .as(alias);
	}

}

package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CountQuartersFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.CountQuartersSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.dialect.Interval;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.forms.StratificationFunctions;
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

public class CountQuartersSqlAggregator implements SelectConverter<CountQuartersSelect>, FilterConverter<CountQuartersFilter, Range.LongRange>, SqlAggregator {

	@Override
	public ConnectorSqlSelects connectorSelect(CountQuartersSelect countQuartersSelect, SelectContext<Connector, ConnectorSqlTables> selectContext) {

		String alias = selectContext.getNameGenerator().selectName(countQuartersSelect);
		ConnectorSqlTables tables = selectContext.getTables();
		SqlFunctionProvider functionProvider = selectContext.getFunctionProvider();

		CommonAggregationSelect<? extends Number> countAggregationSelect;
		if (countQuartersSelect.isSingleColumnDaterange()) {
			Column countColumn = countQuartersSelect.getColumn();
			countAggregationSelect = createSingleColumnAggregationSelect(countColumn, alias, tables, functionProvider);
		}
		else {
			StratificationFunctions stratificationFunctions = StratificationFunctions.create(selectContext.getConversionContext());
			Column startColumn = countQuartersSelect.getStartColumn();
			Column endColumn = countQuartersSelect.getEndColumn();
			countAggregationSelect = createTwoColumnAggregationSelect(startColumn, endColumn, alias, tables, functionProvider, stratificationFunctions);
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

		CommonAggregationSelect<? extends Number> countAggregationSelect;
		if (countQuartersFilter.isSingleColumnDaterange()) {
			Column countColumn = countQuartersFilter.getColumn();
			countAggregationSelect = createSingleColumnAggregationSelect(countColumn, alias, tables, functionProvider);
		}
		else {
			StratificationFunctions stratificationFunctions = StratificationFunctions.create(filterContext.getConversionContext());
			Column startColumn = countQuartersFilter.getStartColumn();
			Column endColumn = countQuartersFilter.getEndColumn();
			countAggregationSelect = createTwoColumnAggregationSelect(startColumn, endColumn, alias, tables, functionProvider, stratificationFunctions);
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

	private CommonAggregationSelect<Integer> createSingleColumnAggregationSelect(
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

		return new CommonAggregationSelect<>(rootSelect, countQuartersAggregation);
	}

	private CommonAggregationSelect<BigDecimal> createTwoColumnAggregationSelect(
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
		Field<Integer> quarterCount = functionProvider.dateDistance(ChronoUnit.MONTHS, quarterStart, nextQuarterStart)
													  .divide(Interval.QUARTER_INTERVAL.getAmount())
													  .as(alias);
		FieldWrapper<Integer> quarterCountReference = new FieldWrapper<>(quarterCount);

		Field<Integer> qualifiedQuarterCount = quarterCountReference.qualify(tables.cteName(ConceptCteStep.EVENT_FILTER)).select();
		FieldWrapper<BigDecimal> quarterCountAggregation = new FieldWrapper<>(DSL.sum(qualifiedQuarterCount).as(alias));

		return new CommonAggregationSelect<>(quarterCountReference, quarterCountAggregation);
	}

}

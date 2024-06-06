package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.sql.Date;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CountQuartersFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.CountQuartersSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
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

public class CountQuartersSqlAggregator implements SelectConverter<CountQuartersSelect>, FilterConverter<CountQuartersFilter, Range.LongRange> {

	@Override
	public ConnectorSqlSelects connectorSelect(CountQuartersSelect countQuartersSelect, SelectContext<Connector, ConnectorSqlTables> selectContext) {

		Column countColumn = countQuartersSelect.getColumn();
		String alias = selectContext.getNameGenerator().selectName(countQuartersSelect);
		ConnectorSqlTables tables = selectContext.getTables();
		SqlFunctionProvider functionProvider = selectContext.getFunctionProvider();

		CommonAggregationSelect<Integer> countAggregationSelect = createCountQuartersAggregationSelect(countColumn, alias, tables, functionProvider);

		String finalPredecessor = tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
		ExtractingSqlSelect<Integer> finalSelect = countAggregationSelect.getGroupBy().qualify(finalPredecessor);

		return ConnectorSqlSelects.builder()
								  .preprocessingSelects(countAggregationSelect.getRootSelects())
								  .aggregationSelect(countAggregationSelect.getGroupBy())
								  .finalSelect(finalSelect)
								  .build();
	}

	@Override
	public SqlFilters convertToSqlFilter(CountQuartersFilter countQuartersFilter, FilterContext<Range.LongRange> filterContext) {

		Column countColumn = countQuartersFilter.getColumn();
		String alias = filterContext.getNameGenerator().selectName(countQuartersFilter);
		ConnectorSqlTables tables = filterContext.getTables();
		SqlFunctionProvider functionProvider = filterContext.getSqlDialect().getFunctionProvider();

		CommonAggregationSelect<Integer> countAggregationSelect = createCountQuartersAggregationSelect(countColumn, alias, tables, functionProvider);
		ConnectorSqlSelects selects = ConnectorSqlSelects.builder()
														 .preprocessingSelects(countAggregationSelect.getRootSelects())
														 .aggregationSelect(countAggregationSelect.getGroupBy())
														 .build();

		Field<Integer> qualifiedCountSelect = countAggregationSelect.getGroupBy().qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER)).select();
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

	private CommonAggregationSelect<Integer> createCountQuartersAggregationSelect(
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

}

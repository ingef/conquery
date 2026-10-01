package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.sql.Date;
import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.QuartersInYearFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.QuartersInYearSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
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
import org.jooq.impl.DSL;

public class QuartersInYearSqlAggregator implements
		SelectConverter<QuartersInYearSelect>,
		FilterConverter<QuartersInYearFilter, Range.LongRange>,
		SqlAggregator {

	@Override
	public ConnectorSqlSelects connectorSelect(QuartersInYearSelect select, SelectContext<ConnectorSqlTables> selectContext) {

		Column column = select.getColumn().resolve();
		String alias = selectContext.getNameGenerator().selectName(select);
		ConnectorSqlTables tables = selectContext.getTables();

		CommonAggregationSelect<Integer> aggregationSelect = createAggregationSelect(
				column,
				alias,
				selectContext.getIds(),
				tables,
				selectContext.getFunctionProvider()
		);
		ExtractingSqlSelect<Integer> finalSelect = aggregationSelect.getGroupBy()
				.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
				.preprocessingSelects(aggregationSelect.getRootSelects())
				.aggregationSelect(aggregationSelect.getGroupBy())
				.finalSelect(finalSelect)
				.build();
	}

	@Override
	public SqlFilters convertToSqlFilter(QuartersInYearFilter filter, FilterContext<Range.LongRange> filterContext) {

		Column column = filter.getColumn().resolve();
		String alias = filterContext.getNameGenerator().selectName(filter);
		ConnectorSqlTables tables = filterContext.getTables();

		CommonAggregationSelect<Integer> aggregationSelect = createAggregationSelect(
				column,
				alias,
				filterContext.getIds(),
				tables,
				filterContext.getFunctionProvider()
		);
		ConnectorSqlSelects selects = ConnectorSqlSelects.builder()
				.preprocessingSelects(aggregationSelect.getRootSelects())
				.aggregationSelect(aggregationSelect.getGroupBy())
				.build();

		Field<Integer> maximumQuarters = aggregationSelect.getGroupBy()
				.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER))
				.select();
		CountCondition countCondition = new CountCondition(maximumQuarters, filterContext.getValue());
		WhereClauses whereClauses = WhereClauses.builder().groupFilter(countCondition).build();

		return new SqlFilters(selects, whereClauses);
	}

	@Override
	public Condition convertForTableExport(QuartersInYearFilter filter, FilterContext<Range.LongRange> filterContext) {

		Column column = filter.getColumn().resolve();
		Field<Date> date = DSL.field(DSL.name(column.getTable().getName(), column.getName()), Date.class);
		Condition oneQuarterMatches = new CountCondition(DSL.inline(1), filterContext.getValue()).condition();

		return date.isNotNull().and(oneQuarterMatches);
	}

	private static CommonAggregationSelect<Integer> createAggregationSelect(
			Column column,
			String alias,
			SqlIdColumns ids,
			ConnectorSqlTables tables,
			SqlFunctionProvider functionProvider
	) {
		ExtractingSqlSelect<Date> rootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), column.getName(), Date.class);
		Field<Date> date = rootSelect.select();
		List<Field<?>> partitionBy = Stream.concat(ids.toFields().stream(), Stream.of(functionProvider.year(date))).toList();
		FieldWrapper<Integer> quarterRank = new FieldWrapper<>(
				DSL.when(
						date.isNotNull(),
						DSL.denseRank().over(DSL.partitionBy(partitionBy).orderBy(functionProvider.yearQuarter(date)))
				).as(alias),
				column.getName()
		);

		String preprocessingCte = tables.cteName(ConceptCteStep.PREPROCESSING);
		Field<Integer> qualifiedQuarterRank = quarterRank.qualify(preprocessingCte).select();
		FieldWrapper<Integer> maximumQuarters = new FieldWrapper<>(DSL.max(qualifiedQuarterRank).as(alias));

		return CommonAggregationSelect.<Integer>builder()
				.rootSelect(quarterRank)
				.groupBy(maximumQuarters)
				.build();
	}
}

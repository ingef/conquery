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
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
				selectContext.getFunctionProvider(),
				selectContext.getNameGenerator()
		);
		ExtractingSqlSelect<Integer> finalSelect = aggregationSelect.getGroupBy()
				.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
				.preprocessingSelects(aggregationSelect.getRootSelects())
				.additionalPredecessor(aggregationSelect.getAdditionalPredecessor())
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
				filterContext.getFunctionProvider(),
				filterContext.getNameGenerator()
		);
		ConnectorSqlSelects selects = ConnectorSqlSelects.builder()
				.preprocessingSelects(aggregationSelect.getRootSelects())
				.additionalPredecessor(aggregationSelect.getAdditionalPredecessor())
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
			SqlFunctionProvider functionProvider,
			NameGenerator nameGenerator
	) {
		ExtractingSqlSelect<Date> rootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), column.getName(), Date.class);
		QueryStep quartersPerYear = createQuartersPerYearCte(rootSelect, alias, ids, tables, functionProvider, nameGenerator);
		FieldWrapper<Integer> maximumQuarters = createMaximumQuartersSelect(quartersPerYear, alias);
		QueryStep maximumQuartersCte = createMaximumQuartersCte(quartersPerYear, maximumQuarters, alias, nameGenerator);

		return CommonAggregationSelect.<Integer>builder()
				.rootSelect(rootSelect)
				.groupBy(maximumQuarters)
				.additionalPredecessor(maximumQuartersCte)
				.build();
	}

	private static QueryStep createQuartersPerYearCte(
			ExtractingSqlSelect<Date> rootSelect,
			String alias,
			SqlIdColumns ids,
			ConnectorSqlTables tables,
			SqlFunctionProvider functionProvider,
			NameGenerator nameGenerator
	) {
		String preprocessingCte = tables.cteName(ConceptCteStep.PREPROCESSING);
		SqlIdColumns qualifiedIds = ids.qualify(preprocessingCte);
		Field<Date> date = rootSelect.qualify(preprocessingCte).select();

		FieldWrapper<Integer> quarterCount = new FieldWrapper<>(
				DSL.nullif(DSL.countDistinct(functionProvider.yearQuarter(date)), 0).as(alias)
		);
		List<Field<?>> groupBy = Stream.concat(qualifiedIds.toFields().stream(), Stream.of(functionProvider.year(date))).toList();

		Selects selects = Selects.builder()
				.ids(qualifiedIds)
				.sqlSelect(quarterCount)
				.build();

		return QueryStep.builder()
				.cteName(nameGenerator.cteStepName(QuartersInYearCteStep.QUARTERS_PER_YEAR, alias))
				.selects(selects)
				.fromTable(QueryStep.toTableLike(preprocessingCte))
				.groupBy(groupBy)
				.build();
	}

	private static FieldWrapper<Integer> createMaximumQuartersSelect(QueryStep quartersPerYear, String alias) {
		Field<Integer> quarterCount = DSL.field(DSL.name(quartersPerYear.getCteName(), alias), Integer.class);
		return new FieldWrapper<>(DSL.max(quarterCount).as(alias));
	}

	private static QueryStep createMaximumQuartersCte(
			QueryStep quartersPerYear,
			FieldWrapper<Integer> maximumQuarters,
			String alias,
			NameGenerator nameGenerator
	) {
		SqlIdColumns ids = quartersPerYear.getQualifiedSelects().getIds();
		Selects selects = Selects.builder()
				.ids(ids)
				.sqlSelect(maximumQuarters)
				.build();

		return QueryStep.builder()
				.cteName(nameGenerator.cteStepName(QuartersInYearCteStep.MAXIMUM_QUARTERS, alias))
				.selects(selects)
				.fromTable(QueryStep.toTableLike(quartersPerYear.getCteName()))
				.groupBy(ids.toFields())
				.predecessor(quartersPerYear)
				.build();
	}

	@Getter
	@RequiredArgsConstructor
	private enum QuartersInYearCteStep implements CteStep {

		QUARTERS_PER_YEAR("quarters_per_year"),
		MAXIMUM_QUARTERS("maximum_quarters");

		private final String suffix;
	}
}

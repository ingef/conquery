package com.bakdata.conquery.sql.conversion.model.select;

import java.sql.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.QuartersInYearSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class QuartersInYearSelectConverter implements SelectConverter<QuartersInYearSelect> {

	@Override
	public ConnectorSqlSelects connectorSelect(QuartersInYearSelect select, SelectContext<ConnectorSqlTables> selectContext) {

		Column column = select.getColumn().resolve();
		String alias = selectContext.getNameGenerator().selectName(select);
		ConnectorSqlTables tables = selectContext.getTables();

		ExtractingSqlSelect<Date> rootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), column.getName(), Date.class);
		QueryStep quartersPerYear = createQuartersPerYearCte(rootSelect, alias, selectContext);
		FieldWrapper<Integer> maximumQuarters = createMaximumQuartersSelect(quartersPerYear, alias);
		QueryStep maximumQuartersCte = createMaximumQuartersCte(quartersPerYear, maximumQuarters, alias, selectContext.getNameGenerator());

		ExtractingSqlSelect<Integer> finalSelect = maximumQuarters.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
				.preprocessingSelect(rootSelect)
				.additionalPredecessor(Optional.of(maximumQuartersCte))
				.finalSelect(finalSelect)
				.build();
	}

	private static QueryStep createQuartersPerYearCte(
			ExtractingSqlSelect<Date> rootSelect,
			String alias,
			SelectContext<ConnectorSqlTables> selectContext
	) {
		String preprocessingCte = selectContext.getTables().cteName(ConceptCteStep.PREPROCESSING);
		SqlIdColumns ids = selectContext.getIds().qualify(preprocessingCte);
		Field<Date> date = rootSelect.qualify(preprocessingCte).select();
		SqlFunctionProvider functionProvider = selectContext.getFunctionProvider();

		FieldWrapper<Integer> quarterCount = new FieldWrapper<>(
				DSL.nullif(DSL.countDistinct(functionProvider.yearQuarter(date)), 0).as(alias)
		);
		List<Field<?>> groupBy = Stream.concat(ids.toFields().stream(), Stream.of(functionProvider.year(date))).toList();

		Selects selects = Selects.builder()
				.ids(ids)
				.sqlSelect(quarterCount)
				.build();

		return QueryStep.builder()
				.cteName(selectContext.getNameGenerator().cteStepName(QuartersInYearCteStep.QUARTERS_PER_YEAR, alias))
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

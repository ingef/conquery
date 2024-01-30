package com.bakdata.conquery.sql.conversion.model.select;

import java.sql.Date;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CountQuartersFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.CountQuartersSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.SelectContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.CountCondition;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import lombok.Value;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Value
public class CountQuartersSqlAggregator implements SqlAggregator {

	SqlSelects sqlSelects;
	WhereClauses whereClauses;

	private CountQuartersSqlAggregator(
			Column column,
			String alias,
			SqlTables<ConnectorCteStep> connectorTables,
			SqlFunctionProvider functionProvider,
			IRange<? extends Number, ?> filterValue
	) {
		ExtractingSqlSelect<Date> rootSelect = new ExtractingSqlSelect<>(
				connectorTables.getPredecessor(ConnectorCteStep.PREPROCESSING),
				column.getName(),
				Date.class
		);

		Field<Date> qualifiedRootSelect = rootSelect.createAliasedReference(connectorTables.getPredecessor(ConnectorCteStep.AGGREGATION_SELECT)).select();
		FieldWrapper<Integer> countQuartersField = new FieldWrapper<>(
				DSL.countDistinct(functionProvider.yearQuarter(qualifiedRootSelect)).as(alias),
				column.getName()
		);

		SqlSelects.SqlSelectsBuilder builder = SqlSelects.builder()
														 .preprocessingSelect(rootSelect)
														 .aggregationSelect(countQuartersField);

		if (filterValue == null) {
			ExtractingSqlSelect<Integer> finalSelect = countQuartersField.createAliasedReference(connectorTables.getPredecessor(ConnectorCteStep.FINAL));
			this.sqlSelects = builder.finalSelect(finalSelect).build();
			this.whereClauses = WhereClauses.builder().build();
		}
		else {
			this.sqlSelects = builder.build();
			Field<Integer> qualified = countQuartersField.createAliasedReference(connectorTables.getPredecessor(ConnectorCteStep.AGGREGATION_FILTER)).select();
			CountCondition countCondition = new CountCondition(qualified, filterValue);
			this.whereClauses = WhereClauses.builder()
											.groupFilter(countCondition)
											.build();
		}
	}

	public static CountQuartersSqlAggregator create(CountQuartersSelect countQuartersSelect, SelectContext selectContext) {
		return new CountQuartersSqlAggregator(
				countQuartersSelect.getColumn(),
				selectContext.getNameGenerator().selectName(countQuartersSelect),
				selectContext.getConnectorTables(),
				selectContext.getParentContext().getSqlDialect().getFunctionProvider(),
				null
		);
	}

	public static CountQuartersSqlAggregator create(CountQuartersFilter countQuartersFilter, FilterContext<Range.LongRange> filterContext) {
		return new CountQuartersSqlAggregator(
				countQuartersFilter.getColumn(),
				filterContext.getNameGenerator().selectName(countQuartersFilter),
				filterContext.getConnectorTables(),
				filterContext.getParentContext().getSqlDialect().getFunctionProvider(),
				filterContext.getValue()
		);
	}

}

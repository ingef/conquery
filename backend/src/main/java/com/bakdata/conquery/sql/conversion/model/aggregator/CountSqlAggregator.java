package com.bakdata.conquery.sql.conversion.model.aggregator;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CountFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.CountSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.SelectContext;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.CountCondition;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import lombok.Value;
import org.jooq.Field;
import org.jooq.impl.DSL;


@Value
public class CountSqlAggregator implements SqlAggregator {

	SqlSelects sqlSelects;
	WhereClauses whereClauses;

	private CountSqlAggregator(
			Column countColumn,
			CountType countType,
			String alias,
			SqlTables<ConnectorCteStep> connectorTables,
			IRange<? extends Number, ?> filterValue
	) {
		ExtractingSqlSelect<?> rootSelect = new ExtractingSqlSelect<>(
				connectorTables.getPredecessor(ConnectorCteStep.PREPROCESSING),
				countColumn.getName(),
				Object.class
		);

		Field<?> qualifiedRootSelect = rootSelect.createAliasReference(connectorTables.getPredecessor(ConnectorCteStep.AGGREGATION_SELECT)).select();
		Field<Integer> countField = countType == CountType.DISTINCT
									? DSL.countDistinct(qualifiedRootSelect)
									: DSL.count(qualifiedRootSelect);
		FieldWrapper<Integer> countGroupBy = new FieldWrapper<>(countField.as(alias), countColumn.getName());

		SqlSelects.SqlSelectsBuilder builder = SqlSelects.builder()
														 .preprocessingSelect(rootSelect)
														 .aggregationSelect(countGroupBy);

		if (filterValue == null) {
			ExtractingSqlSelect<Integer> finalSelect = countGroupBy.createAliasReference(connectorTables.getPredecessor(ConnectorCteStep.FINAL));
			this.sqlSelects = builder.finalSelect(finalSelect).build();
			this.whereClauses = null;
		}
		else {
			this.sqlSelects = builder.build();
			Field<Integer> qualifiedCountSelect =
					countGroupBy.createAliasReference(connectorTables.getPredecessor(ConnectorCteStep.AGGREGATION_FILTER)).select();
			CountCondition countCondition = new CountCondition(qualifiedCountSelect, filterValue);
			this.whereClauses = WhereClauses.builder()
											.groupFilter(countCondition)
											.build();
		}
	}

	public static CountSqlAggregator create(CountSelect countSelect, SelectContext selectContext) {
		return new CountSqlAggregator(
				countSelect.getColumn(),
				CountType.fromBoolean(countSelect.isDistinct()),
				selectContext.getNameGenerator().selectName(countSelect),
				selectContext.getConnectorTables(),
				null
		);
	}

	public static CountSqlAggregator create(CountFilter countFilter, FilterContext<Range.LongRange> filterContext) {
		return new CountSqlAggregator(
				countFilter.getColumn(),
				CountType.fromBoolean(countFilter.isDistinct()),
				filterContext.getNameGenerator().selectName(countFilter),
				filterContext.getConnectorTables(),
				filterContext.getValue()
		);
	}

	@Override
	public SqlFilters getSqlFilters() {
		return new SqlFilters(this.sqlSelects, this.whereClauses);
	}

	public enum CountType {
		DEFAULT,
		DISTINCT;

		public static CountType fromBoolean(boolean value) {
			return value ? DISTINCT : DEFAULT;
		}
	}

}

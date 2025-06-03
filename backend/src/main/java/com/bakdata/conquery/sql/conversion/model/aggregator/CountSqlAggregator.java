package com.bakdata.conquery.sql.conversion.model.aggregator;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CountFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.CountSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.model.filter.CountCondition;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import lombok.NoArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Param;
import org.jooq.impl.DSL;

@NoArgsConstructor
public class CountSqlAggregator implements SelectConverter<CountSelect>, FilterConverter<CountFilter, Range.LongRange>, SqlAggregator {

	@Override
	public ConnectorSqlSelects connectorSelect(CountSelect countSelect, SelectContext<ConnectorSqlTables> selectContext) {

		ConnectorSqlTables tables = selectContext.getTables();
		CountType countType = CountType.fromBoolean(countSelect.isDistinct());
		Column countColumn = countSelect.getColumn().resolve();
		String alias = selectContext.getNameGenerator().selectName(countSelect);

		CommonAggregationSelect<Integer> countAggregationSelect = createCountAggregationSelect(countColumn, countType, alias, tables);

		String finalPredecessor = tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
		ExtractingSqlSelect<Integer> finalSelect = countAggregationSelect.getGroupBy().qualify(finalPredecessor);

		return ConnectorSqlSelects.builder()
								  .preprocessingSelects(countAggregationSelect.getRootSelects())
								  .aggregationSelect(countAggregationSelect.getGroupBy())
								  .finalSelect(finalSelect)
								  .build();
	}

	private CommonAggregationSelect<Integer> createCountAggregationSelect(Column countColumn, CountType countType, String alias, ConnectorSqlTables tables) {

		ExtractingSqlSelect<?> rootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), countColumn.getName(), Object.class);


		Field<?> qualifiedRootSelect = rootSelect.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT)).select();
		Field<Integer> countField = countType == CountType.DISTINCT
									? DSL.countDistinct(qualifiedRootSelect)
									: DSL.count(qualifiedRootSelect);
		FieldWrapper<Integer> countGroupBy = new FieldWrapper<>(DSL.nullif(countField, 0).as(alias), countColumn.getName());

		return CommonAggregationSelect.<Integer>builder()
									  .rootSelect(rootSelect)
									  .groupBy(countGroupBy)
									  .build();
	}

	@Override
	public SqlFilters convertToSqlFilter(CountFilter countFilter, FilterContext<Range.LongRange> filterContext) {

		ConnectorSqlTables tables = filterContext.getTables();
		CountType countType = CountType.fromBoolean(countFilter.isDistinct());
		Column countColumn = countFilter.getColumn().resolve();
		String alias = filterContext.getNameGenerator().selectName(countFilter);

		CommonAggregationSelect<Integer> countAggregationSelect = createCountAggregationSelect(countColumn, countType, alias, tables);
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
	public Condition convertForTableExport(CountFilter countFilter, FilterContext<Range.LongRange> filterContext) {
		Param<Integer> field = DSL.val(1); // no grouping, count is always 1 per row
		return new CountCondition(field, filterContext.getValue()).condition();
	}

	public enum CountType {
		DEFAULT,
		DISTINCT;

		public static CountType fromBoolean(boolean value) {
			return value ? DISTINCT : DEFAULT;
		}
	}

}

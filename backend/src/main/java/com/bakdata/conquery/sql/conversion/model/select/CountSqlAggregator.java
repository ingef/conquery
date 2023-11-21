package com.bakdata.conquery.sql.conversion.model.select;

import java.util.List;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CountFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.CountSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.FilterContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.CountCondition;
import com.bakdata.conquery.sql.conversion.model.filter.Filters;
import lombok.Value;
import org.jooq.Field;
import org.jooq.impl.DSL;


@Value
public class CountSqlAggregator implements SqlAggregator {

	SqlSelects sqlSelects;
	Filters filters;

	private CountSqlAggregator(
			Column countColumn,
			CountType countType,
			String alias,
			SqlTables<ConceptCteStep> conceptTables,
			IRange<? extends Number, ?> filterValue
	) {
		ExtractingSqlSelect<?> rootSelect = new ExtractingSqlSelect<>(
				conceptTables.getPredecessor(ConceptCteStep.PREPROCESSING),
				countColumn.getName(),
				Object.class
		);

		Field<?> qualifiedRootSelect = rootSelect.createAliasedReference(conceptTables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT)).select();
		Field<Integer> countField = countType == CountType.DISTINCT
									? DSL.countDistinct(qualifiedRootSelect)
									: DSL.count(qualifiedRootSelect);
		FieldWrapper<Integer> countGroupBy = new FieldWrapper<>(countField.as(alias), countColumn.getName());

		SqlSelects.SqlSelectsBuilder builder = SqlSelects.builder()
														 .preprocessingSelect(rootSelect)
														 .aggregationSelect(countGroupBy);

		if (filterValue == null) {
			ExtractingSqlSelect<Integer> finalSelect = countGroupBy.createAliasedReference(conceptTables.getPredecessor(ConceptCteStep.FINAL));
			this.sqlSelects = builder.finalSelect(finalSelect).build();
			this.filters = null;
		}
		else {
			this.sqlSelects = builder.build();
			Field<Integer> qualifiedCountSelect = countGroupBy.createAliasedReference(conceptTables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER)).select();
			CountCondition countCondition = new CountCondition(qualifiedCountSelect, filterValue);
			this.filters = Filters.builder()
								  .group(List.of(countCondition))
								  .build();
		}
	}

	public static CountSqlAggregator create(CountSelect countSelect, SelectContext selectContext) {
		return new CountSqlAggregator(
				countSelect.getColumn(),
				CountType.fromBoolean(countSelect.isDistinct()),
				selectContext.getNameGenerator().selectName(countSelect),
				selectContext.getConceptTables(),
				null
		);
	}

	public static CountSqlAggregator create(CountFilter countFilter, FilterContext<Range.LongRange> filterContext) {
		return new CountSqlAggregator(
				countFilter.getColumn(),
				CountType.fromBoolean(countFilter.isDistinct()),
				filterContext.getNameGenerator().selectName(countFilter),
				filterContext.getConceptTables(),
				filterContext.getValue()
		);
	}

	@Override
	public SqlFilters getSqlFilters() {
		return new SqlFilters(this.sqlSelects, this.filters);
	}

	public enum CountType {
		DEFAULT,
		DISTINCT;

		public static CountType fromBoolean(boolean value) {
			return value ? DISTINCT : DEFAULT;
		}
	}

}

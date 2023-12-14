package com.bakdata.conquery.sql.conversion.model.select;

import java.math.BigDecimal;
import java.util.List;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.FilterContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.NumberMapUtil;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.Filters;
import com.bakdata.conquery.sql.conversion.model.filter.SumCondition;
import lombok.Value;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Value
public class SumSqlAggregator implements SqlAggregator {

	SqlSelects sqlSelects;
	Filters filters;

	private SumSqlAggregator(
			Column sumColumn,
			String alias,
			SqlTables<ConceptCteStep> conceptTables,
			IRange<? extends Number, ?> filterValue
	) {
		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(sumColumn.getType());
		ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(
				conceptTables.getPredecessor(ConceptCteStep.PREPROCESSING),
				sumColumn.getName(),
				numberClass
		);

		Field<? extends Number>
				qualifiedRootSelect =
				rootSelect.createAliasedReference(conceptTables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT)).select();
		FieldWrapper<BigDecimal> sumGroupBy = new FieldWrapper<>(DSL.sum(qualifiedRootSelect).as(alias), sumColumn.getName());

		SqlSelects.SqlSelectsBuilder builder = SqlSelects.builder()
														 .preprocessingSelect(rootSelect)
														 .aggregationSelect(sumGroupBy);

		if (filterValue == null) {
			ExtractingSqlSelect<BigDecimal> finalSelect = sumGroupBy.createAliasedReference(conceptTables.getPredecessor(ConceptCteStep.FINAL));
			this.sqlSelects = builder.finalSelect(finalSelect).build();
			this.filters = null;
		}
		else {
			this.sqlSelects = builder.build();
			Field<BigDecimal> qualifiedSumGroupBy = sumGroupBy.createAliasedReference(conceptTables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER)).select();
			SumCondition sumCondition = new SumCondition(qualifiedSumGroupBy, filterValue);
			this.filters = Filters.builder()
								  .group(List.of(sumCondition))
								  .build();
		}
	}

	public static SumSqlAggregator create(SumSelect sumSelect, SelectContext selectContext) {
		Column sumColumn = sumSelect.getColumn();
		String alias = selectContext.getNameGenerator().selectName(sumSelect);
		return new SumSqlAggregator(sumColumn, alias, selectContext.getConceptTables(), null);
	}

	public static SumSqlAggregator create(SumFilter<IRange<? extends Number, ?>> sumFilter, FilterContext<IRange<? extends Number, ?>> filterContext) {
		Column sumColumn = sumFilter.getColumn();
		String alias = filterContext.getNameGenerator().selectName(sumFilter);
		return new SumSqlAggregator(sumColumn, alias, filterContext.getConceptTables(), filterContext.getValue());
	}

}

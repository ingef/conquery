package com.bakdata.conquery.sql.conversion.model.select;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.SelectContext;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.SumCondition;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import lombok.Value;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Value
public class SumSqlAggregator implements SqlAggregator {

	SqlSelects sqlSelects;
	WhereClauses whereClauses;

	private SumSqlAggregator(
			Column sumColumn,
			Column subtractColumn,
			String alias,
			SqlTables<ConnectorCteStep> connectorTables,
			IRange<? extends Number, ?> filterValue
	) {
		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(sumColumn.getType());
		List<ExtractingSqlSelect<? extends Number>> preprocessingSelects = new ArrayList<>();

		ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(
				connectorTables.getPredecessor(ConnectorCteStep.PREPROCESSING),
				sumColumn.getName(),
				numberClass
		);
		preprocessingSelects.add(rootSelect);

		String aggregationSelectPredecessor = connectorTables.getPredecessor(ConnectorCteStep.AGGREGATION_SELECT);
		Field<? extends Number> sumField;
		if (subtractColumn != null) {
			ExtractingSqlSelect<? extends Number> subtractColumnRootSelect = new ExtractingSqlSelect<>(
					connectorTables.getPredecessor(ConnectorCteStep.PREPROCESSING),
					subtractColumn.getName(),
					numberClass
			);
			preprocessingSelects.add(subtractColumnRootSelect);

			Field<? extends Number> qualifiedRootSelect = rootSelect.createAliasedReference(aggregationSelectPredecessor).select();
			Field<? extends Number> qualifiedSubtractRootSelect = subtractColumnRootSelect.createAliasedReference(aggregationSelectPredecessor).select();
			sumField = qualifiedRootSelect.minus(qualifiedSubtractRootSelect);
		}
		else {
			sumField = rootSelect.createAliasedReference(aggregationSelectPredecessor).select();
		}
		FieldWrapper<BigDecimal> sumGroupBy = new FieldWrapper<>(DSL.sum(sumField).as(alias), sumColumn.getName());

		SqlSelects.SqlSelectsBuilder builder = SqlSelects.builder()
														 .preprocessingSelects(preprocessingSelects)
														 .aggregationSelect(sumGroupBy);

		if (filterValue == null) {
			ExtractingSqlSelect<BigDecimal> finalSelect = sumGroupBy.createAliasedReference(connectorTables.getPredecessor(ConnectorCteStep.FINAL));
			this.sqlSelects = builder.finalSelect(finalSelect).build();
			this.whereClauses = null;
		}
		else {
			this.sqlSelects = builder.build();
			Field<BigDecimal> qualifiedSumGroupBy =
					sumGroupBy.createAliasedReference(connectorTables.getPredecessor(ConnectorCteStep.AGGREGATION_FILTER)).select();
			SumCondition sumCondition = new SumCondition(qualifiedSumGroupBy, filterValue);
			this.whereClauses = WhereClauses.builder()
											.groupFilter(sumCondition)
											.build();
		}
	}

	public static SumSqlAggregator create(SumSelect sumSelect, SelectContext selectContext) {
		return new SumSqlAggregator(
				sumSelect.getColumn(),
				sumSelect.getSubtractColumn(),
				selectContext.getNameGenerator().selectName(sumSelect),
				selectContext.getConnectorTables(),
				null
		);
	}

	public static <RANGE extends IRange<? extends Number, ?>> SqlAggregator create(SumFilter<RANGE> sumFilter, FilterContext<RANGE> filterContext) {
		return new SumSqlAggregator(
				sumFilter.getColumn(),
				sumFilter.getSubtractColumn(),
				filterContext.getNameGenerator().selectName(sumFilter),
				filterContext.getConnectorTables(),
				filterContext.getValue()
		);
	}

}

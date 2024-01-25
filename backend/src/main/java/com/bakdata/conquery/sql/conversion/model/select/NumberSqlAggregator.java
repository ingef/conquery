package com.bakdata.conquery.sql.conversion.model.select;

import java.math.BigDecimal;
import java.util.List;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.NumberFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.filter.NumberCondition;
import lombok.Value;
import org.jooq.Field;

@Value
public class NumberSqlAggregator implements SqlAggregator {

	SqlSelects sqlSelects;
	WhereClauses whereClauses;

	public NumberSqlAggregator(
			Column column,
			SqlTables<ConnectorCteStep> conceptTables,
			IRange<? extends Number, ?> filterValue
	) {
		Class<? extends Number> numberClass = NumberMapUtil.NUMBER_MAP.get(column.getType());

		ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(
				conceptTables.getPredecessor(ConnectorCteStep.PREPROCESSING),
				column.getName(),
				numberClass
		);

		Field<Number> eventFilterCtePredecessor = conceptTables.qualifyOnPredecessor(ConnectorCteStep.EVENT_FILTER, rootSelect.aliased());
		NumberCondition condition = new NumberCondition(eventFilterCtePredecessor, filterValue);

		this.sqlSelects = SqlSelects.builder()
									.preprocessingSelects(List.of(rootSelect))
									.build();
		this.whereClauses = WhereClauses.builder()
										.eventFilter(condition)
										.build();
	}

	public static NumberSqlAggregator create(
			NumberFilter<? extends IRange<? extends Number, ?>> numberFilter,
			FilterContext<? extends IRange<? extends Number, ?>> filterContext
	) {
		return new NumberSqlAggregator(
				numberFilter.getColumn(),
				filterContext.getConceptTables(),
				prepareFilterValue(numberFilter.getColumn(), filterContext.getValue())
		);
	}

	/**
	 * If there is a long range filter on a column of type MONEY, the filter value will represent a decimal with the point moved right 2 places right.
	 * <p>
	 * For example, the filter value {@code {min: 1000€, max: 2000€}} will be converted to {@code {min: 10,00€, max: 20,00€}}
	 */
	private static IRange<? extends Number, ?> prepareFilterValue(Column column, IRange<? extends Number, ?> filterValue) {
		if (column.getType() != MajorTypeId.MONEY || !(filterValue instanceof Range.LongRange)) {
			return filterValue;
		}
		Long min = (Long) filterValue.getMin();
		Long max = (Long) filterValue.getMax();
		return Range.LongRange.of(
				BigDecimal.valueOf(min).movePointLeft(2),
				BigDecimal.valueOf(max).movePointLeft(2)
		);
	}

}

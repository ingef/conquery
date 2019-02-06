package com.bakdata.conquery.models.concepts.filters.base;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This filter represents a filter on the sum of one integer column.
 */
@Getter
@Setter
@Slf4j
@CPSType(id = "RANGE", base = Filter.class)
public class RangeFilter extends Filter<FilterValue<? extends IRange<?, ?>>> {

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
//		Select select = getSelect();
//
//		if(select instanceof ColumnSelect) {
//			Column column = ((ColumnSelect) select).getColumn();
//
//			switch (column.getType()) {
//				case MONEY: //see #171  introduce money filter into frontend
//				case INTEGER: {
//					f.setType(FEFilterType.INTEGER_RANGE);
//					return;
//				}
//				case DECIMAL:
//				case REAL: {
//					f.setType(FEFilterType.REAL_RANGE);
//					return;
//				}
//				default:
//					throw new ConceptConfigurationException(getConnector(), "NUMBER filter is incompatible with columns of type " + column.getType());
//			}
//		}
	}

	public Column[] getRequiredColumns() {
		return new Column[0];
	}

	@Override
	public FilterNode createFilter(FilterValue<? extends IRange<?, ?>> filterValue, Aggregator<?> aggregator) {
		return new RangeFilterNode(this, filterValue, aggregator);
	}
}

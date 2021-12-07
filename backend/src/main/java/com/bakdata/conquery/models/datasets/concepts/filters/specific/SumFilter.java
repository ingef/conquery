package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum.DecimalDiffSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum.IntegerDiffSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum.MoneyDiffSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.diffsum.RealDiffSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum.DecimalSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum.IntegerSumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum.MoneySumAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.sum.RealSumAggregator;
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
@CPSType(id = "SUM", base = Filter.class)
public class SumFilter<RANGE extends IRange<? extends Number, ?>> extends Filter<RANGE> {


	@Valid
	@NotNull
	@Getter
	@Setter
	@NsIdRef
	private Column column;

	@Valid
	@Getter
	@Setter
	@NsIdRef
	private Column subtractColumn;

	@Valid
	@Getter
	@Setter
	@NsIdRef
	private Column distinctByColumn;

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		Column column = getColumn();
		f.setType(
				switch (column.getType()) {
					case MONEY -> FEFilterType.MONEY_RANGE;
					case INTEGER -> FEFilterType.INTEGER_RANGE;
					case DECIMAL, REAL -> FEFilterType.REAL_RANGE;

					default -> throw new ConceptConfigurationException(getConnector(), "NUMBER filter is incompatible with columns of type "
																					   + column.getType());
				});
	}

	@Override
	public Column[] getRequiredColumns() {
		return new Column[]{getColumn(), getSubtractColumn(), getDistinctByColumn()};
	}

	@Override
	public FilterNode createFilterNode(RANGE value) {
		ColumnAggregator<?> aggregator = getAggregator();

		if (distinctByColumn != null) {
			return new RangeFilterNode(value, new DistinctValuesWrapperAggregator(aggregator, getDistinctByColumn()));
		}

		if (getColumn().getType() == MajorTypeId.REAL) {
			return new RangeFilterNode(Range.DoubleRange.fromNumberRange(value), aggregator);
		}

		return new RangeFilterNode(value, aggregator);
	}

	private ColumnAggregator<?> getAggregator() {
		if (getSubtractColumn() == null) {
			return switch (getColumn().getType()) {
				case MONEY -> new MoneySumAggregator(getColumn());
				case INTEGER -> new IntegerSumAggregator(getColumn());
				case DECIMAL -> new DecimalSumAggregator(getColumn());
				case REAL -> new RealSumAggregator(getColumn());
				default -> throw new IllegalStateException("No Sum Filter for type " + getColumn().getType().name());
			};
		}
		return switch (getColumn().getType()) {
			case MONEY -> new MoneyDiffSumAggregator(getColumn(), getSubtractColumn());
			case INTEGER -> new IntegerDiffSumAggregator(getColumn(), getSubtractColumn());
			case DECIMAL -> new DecimalDiffSumAggregator(getColumn(), getSubtractColumn());
			case REAL -> new RealDiffSumAggregator(getColumn(), getSubtractColumn());
			default -> throw new IllegalStateException("No Sum Filter for type " + getColumn().getType().name());
		};
	}
}

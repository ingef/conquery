package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FEFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
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
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This filter represents a filter on the sum of one integer column.
 */
@Slf4j
@NoArgsConstructor
@Data
@CPSType(id = "SUM", base = Filter.class)
public class SumFilter<RANGE extends IRange<? extends Number, ?>> extends Filter<RANGE> {

	@NotNull
	@NsIdRef
	private Column column;

	@NsIdRef
	@Nullable
	private Column subtractColumn;

	@NsIdRefCollection
	@NotNull
	private List<Column> distinctByColumn = Collections.emptyList();

	@Override
	public void configureFrontend(FEFilterConfiguration.Top f) throws ConceptConfigurationException {
		switch (getColumn().getType()) {
			case MONEY:
				f.setType(FEFilterType.Fields.MONEY_RANGE);
				return;
			case INTEGER:
				f.setType(FEFilterType.Fields.INTEGER_RANGE);
				return;
			case DECIMAL:
			case REAL: {
				f.setType(FEFilterType.Fields.REAL_RANGE);
				return;
			}
			default:
				throw new ConceptConfigurationException(getConnector(), "NUMBER filter is incompatible with columns of type " + getColumn().getType());
		}
	}

	@Override
	public Column[] getRequiredColumns() {
		List<Column> out = new ArrayList<>();

		out.add(getColumn());
		out.addAll(getDistinctByColumn());

		if(getSubtractColumn() != null){
			out.add(getSubtractColumn());
		}

		return out.toArray(Column[]::new);
	}

	@Override
	public FilterNode createFilterNode(RANGE value) {

		if (!distinctByColumn.isEmpty()) {
			return new RangeFilterNode(value, new DistinctValuesWrapperAggregator(getAggregator(), getDistinctByColumn()));
		}

		if (getColumn().getType() == MajorTypeId.REAL) {
			return new RangeFilterNode(Range.DoubleRange.fromNumberRange(value), getAggregator());
		}

		return new RangeFilterNode(value, getAggregator());
	}

	@JsonIgnore
	private ColumnAggregator<?> getAggregator() {
		if (getSubtractColumn() == null) {
			switch (getColumn().getType()) {
				case MONEY:
					return new MoneySumAggregator(getColumn());
				case INTEGER:
					return new IntegerSumAggregator(getColumn());
				case DECIMAL:
					return new DecimalSumAggregator(getColumn());
				case REAL:
					return new RealSumAggregator(getColumn());
				default:
					throw new IllegalStateException("No Sum Filter for type " + getColumn().getType().name());
			}
		}
		switch (getColumn().getType()) {
			case MONEY:
				return new MoneyDiffSumAggregator(getColumn(), getSubtractColumn());
			case INTEGER:
				return new IntegerDiffSumAggregator(getColumn(), getSubtractColumn());
			case DECIMAL:
				return new DecimalDiffSumAggregator(getColumn(), getSubtractColumn());
			case REAL:
				return new RealDiffSumAggregator(getColumn(), getSubtractColumn());
			default:
				throw new IllegalStateException("No Sum Filter for type " + getColumn().getType().name());
		}
	}
}

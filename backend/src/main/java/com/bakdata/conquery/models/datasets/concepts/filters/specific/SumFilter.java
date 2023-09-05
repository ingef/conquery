package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
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
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) throws ConceptConfigurationException {
		final String type = switch (getColumn().getType()) {
			case MONEY -> FrontendFilterType.Fields.MONEY_RANGE;
			case INTEGER -> FrontendFilterType.Fields.INTEGER_RANGE;
			case DECIMAL, REAL -> FrontendFilterType.Fields.REAL_RANGE;
			default -> throw new ConceptConfigurationException(getConnector(), "NUMBER filter is incompatible with columns of type " + getColumn().getType());
		};

		f.setType(type);
	}

	@Override
	public List<Column> getRequiredColumns() {
		final List<Column> out = new ArrayList<>();

		out.add(getColumn());

		if (distinctByColumn != null) {
			out.addAll(getDistinctByColumn());
		}

		if (getSubtractColumn() != null) {
			out.add(getSubtractColumn());
		}

		return out;
	}

	@Override
	public FilterNode createFilterNode(RANGE value) {
		IRange<? extends Number, ?> range = value;

		// Double values are parsed as BigDecimal, we convert to double if necessary
		if (getColumn().getType() == MajorTypeId.REAL) {
			range = Range.DoubleRange.fromNumberRange(value);
		}

		if (distinctByColumn != null && !distinctByColumn.isEmpty()) {
			return new RangeFilterNode(range, new DistinctValuesWrapperAggregator(getAggregator(), getDistinctByColumn()));
		}

		return new RangeFilterNode(range, getAggregator());
	}

	@JsonIgnore
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

package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
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
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.model.aggregator.SumDistinctSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.aggregator.SumSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.SumCondition;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;

/**
 * This filter represents a filter on the sum of one integer column.
 */
@Slf4j
@NoArgsConstructor
@Data
@CPSType(id = "SUM", base = Filter.class)
public class SumFilter<RANGE extends IRange<? extends Number, ?>> extends Filter<RANGE> {

	private ColumnId column;

	@Nullable
	private ColumnId subtractColumn;

	@NotNull
	private List<ColumnId> distinctByColumn = Collections.emptyList();

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) throws ConceptConfigurationException {
		final MajorTypeId typeId = getColumn().resolve().getType();
		final String type = switch (typeId) {
			case MONEY -> FrontendFilterType.Fields.MONEY_RANGE;
			case INTEGER -> FrontendFilterType.Fields.INTEGER_RANGE;
			case DECIMAL, REAL -> FrontendFilterType.Fields.REAL_RANGE;
			default -> throw new ConceptConfigurationException(getConnector(), "NUMBER filter is incompatible with columns of type " + typeId);
		};

		f.setType(type);
	}

	@Override
	public List<ColumnId> getRequiredColumns() {
		final List<ColumnId> out = new ArrayList<>();

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
		if (getColumn().resolve().getType() == MajorTypeId.REAL) {
			range = Range.DoubleRange.fromNumberRange(value);
		}

		if (distinctByColumn != null && !distinctByColumn.isEmpty()) {
			return new RangeFilterNode(range, new DistinctValuesWrapperAggregator(getAggregator(), getDistinctByColumn().stream()
																														.map(ColumnId::resolve)
																														.toList()));
		}

		return new RangeFilterNode(range, getAggregator());
	}

	@Override
	public SqlFilters convertToSqlFilter(FilterContext<RANGE> filterContext) {
		if (distinctByColumn != null && !distinctByColumn.isEmpty()) {
			return SumDistinctSqlAggregator.create(this, filterContext).getSqlFilters();
		}
		return SumSqlAggregator.create(this, filterContext).getSqlFilters();
	}

	@Override
	public Condition convertForTableExport(FilterContext<RANGE> filterContext) {
		final Column subtrahend = getSubtractColumn() != null ? getSubtractColumn().resolve() : null;
		return SumCondition.onColumn(getColumn().resolve(), subtrahend, filterContext.getValue()).condition();
	}

	@JsonIgnore
	private ColumnAggregator<?> getAggregator() {
		final Column resolvedColumn = getColumn().resolve();
		final MajorTypeId typeId = resolvedColumn.getType();
		if (getSubtractColumn() == null) {

			return switch (typeId) {
				case MONEY -> new MoneySumAggregator(resolvedColumn);
				case INTEGER -> new IntegerSumAggregator(resolvedColumn);
				case DECIMAL -> new DecimalSumAggregator(resolvedColumn);
				case REAL -> new RealSumAggregator(resolvedColumn);
				default -> throw new IllegalStateException("No Sum Filter for type " + typeId.name());
			};
		}

		final Column subtrahend = getSubtractColumn().resolve();
		return switch (resolvedColumn.getType()) {
			case MONEY -> new MoneyDiffSumAggregator(resolvedColumn, subtrahend);
			case INTEGER -> new IntegerDiffSumAggregator(resolvedColumn, subtrahend);
			case DECIMAL -> new DecimalDiffSumAggregator(resolvedColumn, subtrahend);
			case REAL -> new RealDiffSumAggregator(resolvedColumn, subtrahend);
			default -> throw new IllegalStateException("No Sum Filter for type " + typeId.name());
		};
	}
}

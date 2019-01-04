package com.bakdata.conquery.models.query.aggregators.filter;

import static com.bakdata.conquery.models.query.queryplan.OpenResult.MAYBE;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.concepts.filters.specific.SlidingSumFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.OpenResult;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SlidingSumFilterNode extends FilterNode<FilterValue.CQRealRangeFilter, SlidingSumFilter> {

	private double sum = 0;

	public SlidingSumFilterNode(SlidingSumFilter slidingSumFilter, FilterValue.CQRealRangeFilter filterValue) {
		super(slidingSumFilter, filterValue);
	}


	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		return new SlidingSumFilterNode(filter, filterValue);
	}

	@Override
	protected OpenResult nextEvent(Block block, int event) {
		for (Column col : filter.getRequiredColumns()) {
			if (!block.has(event, col)) {
				return MAYBE;
			}
		}

		CDateRange dateRange = block.getDateRange(event, filter.getDateRangeColumn());

		double maxDays = block.getInteger(event, filter.getMaximumDaysColumn());

		double value = block.getReal(event, filter.getValueColumn());

		double durationInDays = dateRange.getNumberOfDays();
		double out = value * (Math.min(durationInDays, maxDays) / durationInDays);

		// Current Formula: value * (min(len(dateRange), maxDays) / len(dateRange))
		// Supposed Formula from the sources: value * (min(len(intersect(dateRange, valid)), maxDays) / len(dateRange))

		sum += out;

		return MAYBE;
	}

	@Override
	public boolean isContained() {
		return filterValue.getValue().contains(sum);
	}
}

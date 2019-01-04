package com.bakdata.conquery.models.query.aggregators.filter;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.concepts.filters.specific.QuartersInYearFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.OpenResult;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

/**
 * Entity is included when the the number of quarters with events is within a specified range.
 */
public class QuartersInYearFilterNode extends FilterNode<FilterValue.CQIntegerRangeFilter, QuartersInYearFilter> {

	private final Map<Integer, BitSet> quartersInYear;

	public QuartersInYearFilterNode(QuartersInYearFilter quartersInYearFilter, FilterValue.CQIntegerRangeFilter filterValue) {
		super(quartersInYearFilter, filterValue);
		quartersInYear = new HashMap<>();
	}

	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		return new QuartersInYearFilterNode(filter, filterValue);
	}

	@Override
	protected OpenResult nextEvent(Block block, int event) {
		if (!block.has(event, filter.getColumn())) {
			return OpenResult.MAYBE;
		}

		int dateRange = block.getDate(event, filter.getColumn());
		LocalDate date = CDate.toLocalDate(dateRange);

		BitSet quarters = quartersInYear.computeIfAbsent(date.getYear(), year -> new BitSet(4));

		int quarter = date.get(IsoFields.QUARTER_OF_YEAR);
		quarters.set(quarter - 1);

		// Exit early when cardinality of a single exceeds max
		if (quarters.cardinality() > filterValue.getValue().getMax()) {
			return OpenResult.NOT_INCLUDED;
		}

		return OpenResult.MAYBE;
	}

	@Override
	public boolean isContained() {
		OptionalInt max = quartersInYear.values().stream()
										.mapToInt(BitSet::cardinality)
										.max();
		return max.isPresent() && filterValue.getValue().contains(max.getAsInt());
	}
}

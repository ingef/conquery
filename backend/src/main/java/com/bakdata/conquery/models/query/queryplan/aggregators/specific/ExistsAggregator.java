package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.RequiredArgsConstructor;

/**
 * Helper Aggregator, returning if it was used at least once.
 */
@RequiredArgsConstructor
public class ExistsAggregator implements Aggregator<Boolean> {

	private final Set<TableId> requiredTables;
	private final Set<FilterNode<?>> filters = new HashSet<>();

	@Override
	public void acceptEvent(Bucket bucket, int event) {  }

	@Override
	public Boolean getAggregationResult() {
		return filters.stream().allMatch(FilterNode::isContained);
	}
	
	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		requiredTables.addAll(this.requiredTables);
	}

	@Override
	public ExistsAggregator doClone(CloneContext ctx) {
		final List<FilterNode<?>> clonedNodes = new ArrayList<>(filters);
		clonedNodes.replaceAll(ctx::clone);

		final ExistsAggregator aggregator = new ExistsAggregator(requiredTables);

		aggregator.addFilters(clonedNodes);

		return aggregator;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.BOOLEAN;
	}
	
	@Override
	public String toString(){
		return getClass().getSimpleName();
	}

	public void addFilters(Collection<? extends FilterNode<?>> filters) {
		this.filters.addAll(filters);
	}
}

package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class FiltersNode extends QPNode {

	private boolean hit = false;

	@Getter
	private final List<? extends FilterNode<?>> filters;
	private final List<EventFilterNode<?>> eventFilters;
	private final List<Aggregator<?>> aggregators;


	public FiltersNode(List<FilterNode<?>> filters, List<Aggregator<?>> aggregators) {
		this.aggregators = aggregators;
		this.filters = filters;

		eventFilters = new ArrayList<>(filters.size());

		for (FilterNode<?> filter : filters) {
			if (!(filter instanceof EventFilterNode)) {
				continue;
			}

			eventFilters.add((EventFilterNode<?>) filter);
		}
	}


	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		super.nextTable(ctx, currentTable);
		filters.forEach(f -> f.nextTable(ctx, currentTable));
		aggregators.forEach(a -> a.nextTable(ctx, currentTable));
	}
	
	@Override
	public void nextBlock(Bucket bucket) {
		super.nextBlock(bucket);
		filters.forEach(f -> f.nextBlock(bucket));
		aggregators.forEach(a -> a.nextBlock(bucket));
	}
	
	@Override
	public final void acceptEvent(Bucket bucket, int event) {
		for(EventFilterNode<?> f : eventFilters) {
			if (!f.checkEvent(bucket, event)) {
				return;
			}
		}

		filters.forEach(f -> f.acceptEvent(bucket, event));
		aggregators.forEach(a -> a.acceptEvent(bucket, event));

		hit = true;
	}

	@Override
	public boolean isContained() {
		for(FilterNode<?> f : filters) {
			if (!f.isContained()) {
				return false;
			}
		}

		return hit;
	}
	
	@Override
	public FiltersNode doClone(CloneContext ctx) {
		List<FilterNode<?>> _filters = new ArrayList<>(filters);
		_filters.replaceAll(ctx::clone);

		List<EventFilterNode<?>> _eventFilters = new ArrayList<>(eventFilters);
		_eventFilters.replaceAll(ctx::clone);

		List<Aggregator<?>> _aggregators = new ArrayList<>(aggregators);
		_aggregators.replaceAll(ctx::clone);

		return new FiltersNode(_filters, _eventFilters, _aggregators);
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		super.collectRequiredTables(requiredTables);

		filters.forEach(f -> f.collectRequiredTables(requiredTables));
		aggregators.forEach(a -> a.collectRequiredTables(requiredTables));
	}
	
	@Override
	public boolean isOfInterest(Bucket bucket) {
		return true;
	}
}

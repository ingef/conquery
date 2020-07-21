package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;


public class FiltersNode extends QPChainNode {
	@Getter
	private final List<? extends FilterNode<?>> filters;

	private final List<EventFilterNode<?>> eventFilters;

	public FiltersNode(List<FilterNode<?>> filters, QPNode child) {
		super(child);
		this.filters = filters;

		eventFilters = new ArrayList<>(filters.size());

		for (FilterNode<?> filter : filters) {
			if (!(filter instanceof EventFilterNode)) {
				continue;
			}

			eventFilters.add((EventFilterNode<?>) filter);
		}
	}

	protected FiltersNode(List<FilterNode<?>> filters, List<EventFilterNode<?>> eventFilters, QPNode child) {
		super(child);
		this.filters = filters;
		this.eventFilters = eventFilters;
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		super.nextTable(ctx, currentTable);
		for(FilterNode<?> f:filters) {
			f.nextTable(ctx, currentTable);
		}
	}
	
	@Override
	public void nextBlock(Bucket bucket) {
		super.nextBlock(bucket);
		for(FilterNode<?> f:filters) {
			f.nextBlock(bucket);
		}
	}
	
	@Override
	public final void acceptEvent(Bucket bucket, int event) {
		for(EventFilterNode<?> f : eventFilters) {
			if (!f.checkEvent(bucket, event)) {
				return;
			}
		}

		for(FilterNode<?> f : filters) {
			f.acceptEvent(bucket, event);
		}

		getChild().acceptEvent(bucket, event);
	}

	@Override
	public boolean isContained() {
		for(FilterNode<?> f : filters) {
			if (!f.isContained()) {
				return false;
			}
		}
		return getChild().isContained();
	}
	
	@Override
	public FiltersNode doClone(CloneContext ctx) {
		List<FilterNode<?>> _filters = new ArrayList<>(filters);
		_filters.replaceAll(fn -> ctx.clone((FilterNode<?>) fn));

		List<EventFilterNode<?>> _eventFilters = new ArrayList<>(eventFilters);
		_eventFilters.replaceAll(fn -> (EventFilterNode<?>) ctx.clone((FilterNode<?>) fn));

		return new FiltersNode(_filters, _eventFilters, ctx.clone((QPNode) getChild()));
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		super.collectRequiredTables(requiredTables);
		for(FilterNode<?> f:filters) {
			f.collectRequiredTables(requiredTables);
		}
	}
	
	@Override
	public boolean isOfInterest(Bucket bucket) {
		return true;
	}
}

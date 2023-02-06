package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@ToString(of = {"filters", "aggregators"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FiltersNode extends QPNode {

	private boolean hit = false;

	@Getter
	@Setter(AccessLevel.PRIVATE)
	private List<? extends FilterNode<?>> filters;

	@Setter(AccessLevel.PRIVATE)
	private List<Aggregator<?>> aggregators;

	@Setter(AccessLevel.PRIVATE)
	private List<EventFilterNode<?>> eventFilters;


	@Setter(AccessLevel.PRIVATE)
	private List<Aggregator<CDateSet>> eventDateAggregators;

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		super.init(entity, context);

		hit = false;

		filters.forEach(child -> child.init(entity, context));
		aggregators.forEach(child -> child.init(entity, context));
		eventFilters.forEach(child -> child.init(entity, context));
		eventDateAggregators.forEach(child -> child.init(entity, context));
	}

	public static FiltersNode create(List<? extends FilterNode<?>> filters, List<Aggregator<?>> aggregators, List<Aggregator<CDateSet>> eventDateAggregators) {
		if (filters.isEmpty() && aggregators.isEmpty()) {
			throw new IllegalStateException("Unable to create FilterNode without filters or aggregators.");
		}

		final List<EventFilterNode<?>> eventFilters = new ArrayList<>(filters.size());

		// Select only Event Filtering nodes as they are used differently.
		for (FilterNode<?> filter : filters) {
			if (!(filter instanceof EventFilterNode)) {
				continue;
			}

			eventFilters.add((EventFilterNode<?>) filter);
		}

		final FiltersNode filtersNode = new FiltersNode();
		filtersNode.setAggregators(aggregators);
		filtersNode.setFilters(filters);
		filtersNode.setEventFilters(eventFilters);
		filtersNode.setEventDateAggregators(eventDateAggregators);

		return filtersNode;
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
		for (EventFilterNode<?> f : eventFilters) {
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
		for (FilterNode<?> f : filters) {
			if (!f.isContained()) {
				return false;
			}
		}

		return hit;
	}

	@Override
	public Collection<Aggregator<CDateSet>> getDateAggregators() {
		return eventDateAggregators;
	}

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		super.collectRequiredTables(requiredTables);

		filters.forEach(f -> f.collectRequiredTables(requiredTables));
		aggregators.forEach(a -> a.collectRequiredTables(requiredTables));
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		for (FilterNode<?> filter : filters) {
			if (filter.isOfInterest(bucket)) {
				return true;
			}
		}

		for (Aggregator<?> aggregator : aggregators) {
			if (aggregator.isOfInterest(bucket)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		for (FilterNode<?> filter : filters) {
			if (filter.isOfInterest(entity)) {
				return true;
			}
		}

		for (Aggregator<?> aggregator : aggregators) {
			if (aggregator.isOfInterest(entity)) {
				return true;
			}
		}

		return false;
	}


}

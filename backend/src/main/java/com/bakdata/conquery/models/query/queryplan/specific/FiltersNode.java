package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.*;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.EventDateUnionAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
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

	@Getter @Setter(AccessLevel.PRIVATE)
	private List<? extends FilterNode<?>> filters;

	@Setter(AccessLevel.PRIVATE)
	private List<Aggregator<?>> aggregators;

	@Setter(AccessLevel.PRIVATE)
	private List<EventFilterNode<?>> eventFilters;


	@Setter(AccessLevel.PRIVATE)
	private Set<Aggregator<Collection<CDateRange>>> eventDateAggregators;


	public static FiltersNode create(List<? extends FilterNode<?>> filters, List<Aggregator<?>> aggregators) {
		if(filters.isEmpty() && aggregators.isEmpty()) {
			throw new IllegalStateException("Unable to create FilterNode without filters or aggregators.");
		}
		
		final ArrayList<EventFilterNode<?>> eventFilters = new ArrayList<>(filters.size());

		// Select only Event Filtering nodes as they are used differently.
		for (FilterNode<?> filter : filters) {
			if (!(filter instanceof EventFilterNode)) {
				continue;
			}

			eventFilters.add((EventFilterNode<?>) filter);
		}

		Set<Aggregator<Collection<CDateRange>>> eventDateAggregators = new HashSet<>();
		for (Aggregator<?> aggregator: aggregators) {
			if(aggregator instanceof EventDateUnionAggregator) {
				eventDateAggregators.add((EventDateUnionAggregator) aggregator);
			}
		}

		final FiltersNode filtersNode = new FiltersNode();
		filtersNode.setAggregators(aggregators);
		filtersNode.setFilters(filters);
		filtersNode.setEventFilters(eventFilters);

		return filtersNode;
	}


	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
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
	public Collection<Aggregator<Collection<CDateRange>>> getDateAggregators() {
		return eventDateAggregators;
	}

	@Override
	public FiltersNode doClone(CloneContext ctx) {
		final FiltersNode clone = new FiltersNode();

		List<FilterNode<?>> filters = new ArrayList<>(this.filters);
		filters.replaceAll(ctx::clone);

		clone.setFilters(filters);

		List<EventFilterNode<?>> eventFilters = new ArrayList<>(this.eventFilters);
		eventFilters.replaceAll(ctx::clone);
		clone.setEventFilters(eventFilters);

		List<Aggregator<?>> aggregators = new ArrayList<>(this.aggregators);
		aggregators.replaceAll(ctx::clone);

		clone.setAggregators(aggregators);

		return clone;
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		super.collectRequiredTables(requiredTables);

		filters.forEach(f -> f.collectRequiredTables(requiredTables));
		aggregators.forEach(a -> a.collectRequiredTables(requiredTables));
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		for (FilterNode<?> filter : filters) {
			if(filter.isOfInterest(bucket)){
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
			if(filter.isOfInterest(entity)){
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

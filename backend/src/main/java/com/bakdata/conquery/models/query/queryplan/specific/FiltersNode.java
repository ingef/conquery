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
import com.bakdata.conquery.models.query.queryplan.filter.AggregationResultFilterNode;
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
	private List<AggregationResultFilterNode<?,?>> aggregationFilters;


	@Setter(AccessLevel.PRIVATE)
	private List<Aggregator<CDateSet>> eventDateAggregators;

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		super.init(entity, context);

		hit = false;

		for (FilterNode<?> filter : filters) {
			filter.init(entity, context);
		}

		for (Aggregator<?> aggregator : aggregators) {
			aggregator.init(entity, context);
		}

		for (EventFilterNode<?> eventFilter : eventFilters) {
			eventFilter.init(entity, context);
		}

		for (Aggregator<CDateSet> child : eventDateAggregators) {
			child.init(entity, context);
		}
	}

	public static FiltersNode create(List<? extends FilterNode<?>> filters, List<Aggregator<?>> aggregators, List<Aggregator<CDateSet>> eventDateAggregators) {
		if (filters.isEmpty() && aggregators.isEmpty()) {
			throw new IllegalStateException("Unable to create FilterNode without filters or aggregators.");
		}

		final List<EventFilterNode<?>> eventFilters = new ArrayList<>(filters.size());
		final List<AggregationResultFilterNode<?,?>> aggregationFilters = new ArrayList<>(filters.size());

		// Event and AggregationResultFilterNodes are used differently
		for (FilterNode<?> filter : filters) {
			if (filter instanceof EventFilterNode<?> ef) {
				eventFilters.add(ef);
			}
			else if (filter instanceof AggregationResultFilterNode<?,?> af){
				aggregationFilters.add(af);
			}
		}

		final FiltersNode filtersNode = new FiltersNode();
		filtersNode.setAggregators(aggregators);
		filtersNode.setFilters(filters);
		filtersNode.setEventFilters(eventFilters);
		filtersNode.setEventDateAggregators(eventDateAggregators);
		filtersNode.setAggregationFilters(aggregationFilters);

		return filtersNode;
	}


	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		super.nextTable(ctx, currentTable);

		for (FilterNode<?> f : filters) {
			f.nextTable(ctx, currentTable);
		}

		for (Aggregator<?> a : aggregators) {
			a.nextTable(ctx, currentTable);
		}
	}

	@Override
	public void nextBlock(Bucket bucket) {
		super.nextBlock(bucket);

		for (FilterNode<?> f : filters) {
			f.nextBlock(bucket);
		}

		for (Aggregator<?> a : aggregators) {
			a.nextBlock(bucket);
		}
	}

	@Override
	public final boolean acceptEvent(Bucket bucket, int event) {
		for (EventFilterNode<?> f : eventFilters) {
			if (!f.checkEvent(bucket, event)) {
				return false;
			}
		}

		for (AggregationResultFilterNode<?,?> f : aggregationFilters) {
			f.acceptEvent(bucket, event);
		}
		for (Aggregator<?> a : aggregators) {
			a.consumeEvent(bucket, event);
		}

		hit = true;

		return true;
	}

	@Override
	public boolean isContained() {
		for (AggregationResultFilterNode<?,?> f : aggregationFilters) {
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

		for (FilterNode<?> f : filters) {
			f.collectRequiredTables(requiredTables);
		}

		for (Aggregator<?> a : aggregators) {
			a.collectRequiredTables(requiredTables);
		}
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		if(!bucket.containsEntity(entity.getId())){
			return false;
		}

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

package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.EventDateUnionAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.AggregationResultFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;


@ToString(of = {"filters", "aggregators"})
@RequiredArgsConstructor
public class FiltersNode extends QPNode {

	@Getter
	private final List<? extends FilterNode<?>> filters;
	private final List<Aggregator<?>> aggregators;
	private final List<EventFilterNode<?>> eventFilters;
	private final List<AggregationResultFilterNode<?, ?>> aggregationFilters;
	private final EventDateUnionAggregator eventDateAggregator;
	private boolean hit = false;

	public static FiltersNode create(List<? extends FilterNode<?>> filters, List<Aggregator<?>> aggregators, EventDateUnionAggregator eventDateAggregator) {
		if (filters.isEmpty() && aggregators.isEmpty()) {
			throw new IllegalStateException("Unable to create FilterNode without filters or aggregators.");
		}

		final List<EventFilterNode<?>> eventFilters = new ArrayList<>(filters.size());
		final List<AggregationResultFilterNode<?, ?>> aggregationFilters = new ArrayList<>(filters.size());

		// Event and AggregationResultFilterNodes are used differently
		for (FilterNode<?> filter : filters) {
			switch (filter) {
				case EventFilterNode<?> ef -> eventFilters.add(ef);
				case AggregationResultFilterNode<?, ?> af -> aggregationFilters.add(af);
			}
		}

		return new FiltersNode(
				filters,
				aggregators,
				eventFilters,
				aggregationFilters,
				eventDateAggregator
		);
	}

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

		if (eventDateAggregator != null) {
			eventDateAggregator.init(entity, context);
		}
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

		for (AggregationResultFilterNode<?, ?> f : aggregationFilters) {
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
		for (AggregationResultFilterNode<?, ?> f : aggregationFilters) {
			if (!f.isContained()) {
				return false;
			}
		}

		return hit;
	}

	@Override
	public Collection<Aggregator<CDateSet>> getDateAggregators() {
		if (eventDateAggregator == null) {
			return Collections.emptyList();
		}

		return List.of(eventDateAggregator);
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
		if (!bucket.containsEntity(entity.getId())) {
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

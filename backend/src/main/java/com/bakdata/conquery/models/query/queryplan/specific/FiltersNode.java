package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.*;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.filter.AggregationResultFilterNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.EventDateUnionAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@ToString(of = {"filters", "aggregators"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FiltersNode extends QPNode {

	private boolean hit = false;

	@Setter(AccessLevel.PRIVATE)
	private List<Aggregator<?>> aggregators;

	@Setter(AccessLevel.PRIVATE)
	private List<EventFilterNode<?>> eventFilters;
	@Setter(AccessLevel.PRIVATE)
	private List<AggregationResultFilterNode<?, ?>> aggregationFilters;


	@Setter(AccessLevel.PRIVATE)
	private List<Aggregator<CDateSet>> eventDateAggregators;


	public static FiltersNode create(List<? extends FilterNode<?>> filters, List<Aggregator<?>> aggregators) {
		if(filters.isEmpty() && aggregators.isEmpty()) {
			throw new IllegalStateException("Unable to create FilterNode without filters or aggregators.");
		}
		
		final ArrayList<EventFilterNode<?>> eventFilters = new ArrayList<>(filters.size());
		final List<AggregationResultFilterNode<?, ?>> aggregationFilters = new ArrayList<>(filters.size());

		// Select only Event Filtering nodes as they are used differently.
		for (FilterNode<?> filter : filters) {
			if ((filter instanceof EventFilterNode)) {
				eventFilters.add((EventFilterNode<?>) filter);
				continue;
			}
			if ((filter instanceof AggregationResultFilterNode)) {
				aggregationFilters.add((AggregationResultFilterNode<?,?>) filter);
				continue;
			}
			throw new IllegalArgumentException("Unknown filter type: " + filter.getClass().getName());

		}

		List<Aggregator<CDateSet>> eventDateAggregators = new ArrayList<>();
		for (Aggregator<?> aggregator: aggregators) {
			if(aggregator instanceof EventDateUnionAggregator) {
				eventDateAggregators.add((EventDateUnionAggregator) aggregator);
			}
		}

		final FiltersNode filtersNode = new FiltersNode();
		filtersNode.setAggregators(aggregators);
		filtersNode.setAggregationFilters(aggregationFilters);
		filtersNode.setEventFilters(eventFilters);
		filtersNode.setEventDateAggregators(eventDateAggregators);

		return filtersNode;
	}


	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		super.nextTable(ctx, currentTable);
		eventFilters.forEach(f -> f.nextTable(ctx, currentTable));
		aggregationFilters.forEach(f -> f.nextTable(ctx, currentTable));
		aggregators.forEach(a -> a.nextTable(ctx, currentTable));
	}
	
	@Override
	public void nextBlock(Bucket bucket) {
		super.nextBlock(bucket);
		eventFilters.forEach(f -> f.nextBlock(bucket));
		aggregationFilters.forEach(f -> f.nextBlock(bucket));
		aggregators.forEach(a -> a.nextBlock(bucket));
	}

	@Override
	public final Optional<Boolean> eventFiltersApply(Bucket bucket, int event) {
		if (eventFilters.isEmpty()) {
			return Optional.empty();
		}
		// On a table/connector all event filters must apply similar to AndNode
		for(EventFilterNode<?> f : eventFilters) {
			final Optional<Boolean> result = f.eventFiltersApply(bucket, event);
			if (!result.orElse(true)) {
				log.warn("A filter didn't apply for an event of entity {}", getEntity().getId());
				return result;
			}
		}
		log.warn("All filters applied for an event of entity {}", getEntity().getId());
		return Optional.of(Boolean.TRUE);
	}
	
	@Override
	public final void acceptEvent(Bucket bucket, int event) {

		log.warn("Accepting events for entity {}", getEntity().getId());

		aggregationFilters.forEach(f -> f.acceptEvent(bucket, event));
		aggregators.forEach(a -> a.acceptEvent(bucket, event));

		final Optional<Boolean> result = eventFiltersApply(bucket, event);
		if (result.orElse(true) ) {
			hit = true;
		}
	}

	@Override
	public Optional<Boolean> aggregationFiltersApply() {

		if (aggregationFilters.isEmpty()) {
			return Optional.of(hit);
		}

		for(FilterNode<?> f : aggregationFilters) {
			if (!f.isContained()) {
				log.warn("A filter for entity {} was not hit.", getEntity().getId());
				return Optional.of(Boolean.FALSE);
			}
		}


		return Optional.of(Boolean.TRUE);
	}

	@Override
	public Collection<Aggregator<CDateSet>> getDateAggregators() {
		return eventDateAggregators;
	}

	@Override
	public FiltersNode doClone(CloneContext ctx) {
		final FiltersNode clone = new FiltersNode();

		List<EventFilterNode<?>> eventFilters = new ArrayList<>(this.eventFilters);
		eventFilters.replaceAll(ctx::clone);
		clone.setEventFilters(eventFilters);

		List<AggregationResultFilterNode<?, ?>> aggregationFilters = new ArrayList<>(this.aggregationFilters);
		aggregationFilters.replaceAll(ctx::clone);
		clone.setAggregationFilters(aggregationFilters);

		List<Aggregator<?>> aggregators = new ArrayList<>(this.aggregators);
		aggregators.replaceAll(ctx::clone);
		clone.setAggregators(aggregators);

		List<Aggregator<CDateSet>> eventDateAggregators = new ArrayList<>(this.eventDateAggregators);
		eventDateAggregators.replaceAll(ctx::clone);
		clone.setEventDateAggregators(eventDateAggregators);

		return clone;
	}

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		super.collectRequiredTables(requiredTables);

		eventFilters.forEach(f -> f.collectRequiredTables(requiredTables));
		aggregationFilters.forEach(f -> f.collectRequiredTables(requiredTables));
		aggregators.forEach(a -> a.collectRequiredTables(requiredTables));
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		for (FilterNode<?> filter : eventFilters) {
			if(filter.isOfInterest(bucket)){
				return true;
			}
		}

		for (FilterNode<?> filter : aggregationFilters) {
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
		for (FilterNode<?> filter : eventFilters) {
			if(filter.isOfInterest(entity)){
				return true;
			}
		}

		for (FilterNode<?> filter : aggregationFilters) {
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

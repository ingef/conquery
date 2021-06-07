package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.concept.TableExportQuery;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@ToString(of = {"filters", "aggregators"})
@RequiredArgsConstructor
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



	public static FiltersNode create(List<EventFilterNode<?>> eventFilters, List<? extends FilterNode<?>> filters, List<Aggregator<?>> aggregators, List<Aggregator<CDateSet>> eventDateAggregators) {


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
		if (!checkEvent(bucket, event)) {
			return;
		}

		filters.forEach(f -> f.acceptEvent(bucket, event));
		aggregators.forEach(a -> a.acceptEvent(bucket, event));

		hit = true;
	}

	public boolean checkEvent(Bucket bucket, int event) {
		for(EventFilterNode<?> f : eventFilters) {
			if (!f.checkEvent(bucket, event)) {
				return false;
			}
		}
		return true;
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

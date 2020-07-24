package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.EventIterating;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;


public class FiltersNode extends QPChainNode implements EventIterating {
	@Getter
	private final List<FilterNode<?>> filters;

	public FiltersNode(List<FilterNode<?>> filters, QPNode child) {
		super(child);
		this.filters = filters;
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
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
	public final void nextEvent(Bucket bucket, int event) {
		for(FilterNode<?> f : filters) {
			if (!f.checkEvent(bucket, event)) {
				return;
			}
		}

		for(FilterNode<?> f : filters) {
			f.acceptEvent(bucket, event);
		}

		getChild().nextEvent(bucket, event);
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
		List<FilterNode<?>> copy = new ArrayList<>(filters);
		copy.replaceAll(fn->fn.clone(ctx));

		return new FiltersNode(copy, getChild().clone(ctx));
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

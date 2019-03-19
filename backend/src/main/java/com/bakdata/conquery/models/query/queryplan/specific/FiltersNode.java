package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.EventIterating;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;


public class FiltersNode extends QPChainNode implements EventIterating {

	private final List<FilterNode<?>> filters;

	public FiltersNode(List<FilterNode<?>> filters, QPNode child) {
		super(child);
		this.filters = filters;
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		super.nextTable(ctx, currentTable);
		for(FilterNode<?> f:filters) {
			f.nextTable(ctx, currentTable);
		}
	}
	
	@Override
	public void nextBlock(Block block) {
		super.nextBlock(block);
		for(FilterNode<?> f:filters) {
			f.nextBlock(block);
		}
	}
	
	@Override
	public final void nextEvent(Block block, int event) {
		for(FilterNode<?> f : filters) {
			if (!f.checkEvent(block, event)) {
				return;
			}
		}

		for(FilterNode<?> f : filters) {
			f.acceptEvent(block, event);
		}

		getChild().nextEvent(block, event);
	}

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
}

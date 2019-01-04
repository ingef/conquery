package com.bakdata.conquery.models.query.filter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;


public class AndFilterNode<FILTER extends Filter<FilterValue<?>>> extends FilterNode<FilterValue<?>, FILTER> {

	private final Collection<FilterNode<?, ?>> filterNodes;

	public AndFilterNode(FILTER filter, FilterNode<?, ?>... filterNodes) {
		super(filter, null);
		this.filterNodes = Arrays.asList(filterNodes);
	}


	public AndFilterNode(FILTER filter, Collection<FilterNode<?, ?>> filterNodes) {
		super(filter, null);
		this.filterNodes = filterNodes;
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		for (FilterNode<?, ?> filterNode : filterNodes) {
			filterNode.collectRequiredTables(requiredTables);
		}
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		for (FilterNode<?, ?> filterNode : filterNodes) {
			filterNode.nextTable(ctx, currentTable);
		}
	}

	@Override
	public void nextBlock(Block block) {
		for (FilterNode<?, ?> filterNode : filterNodes) {
			filterNode.nextBlock(block);
		}
	}

	@Override
	public FilterNode<?, ?> clone(QueryPlan plan, QueryPlan clone) {
		return new AndFilterNode<>(filter, filterNodes.stream().map(fn -> fn.clone(plan, clone)).collect(Collectors.toList()));
	}

	@Override
	public boolean checkEvent(Block block, int event) {
		for (FilterNode<?, ?> filterNode : filterNodes) {
			if (!filterNode.checkEvent(block, event)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void acceptEvent(Block block, int event) {
		for (FilterNode<?, ?> filterNode : filterNodes) {
			filterNode.acceptEvent(block, event);
		}
	}

	@Override
	public boolean isContained() {
		for (FilterNode<?, ?> filterNode : filterNodes) {
			if (!filterNode.isContained()) {
				return false;
			}
		}

		return true;
	}
}

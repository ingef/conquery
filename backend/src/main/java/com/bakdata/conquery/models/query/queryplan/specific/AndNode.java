package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.queryplan.DateAggregator;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QPParentNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.tuple.Pair;

public class AndNode extends QPParentNode {

	public AndNode(List<QPNode> children, DateAggregationAction action) {
		super(children, action);
	}

	private AndNode(List<QPNode> children, ListMultimap<Table, QPNode> childMap, DateAggregator dateAggregator) {
		super(children, childMap, dateAggregator);
	}

	@Override
	public QPNode doClone(CloneContext ctx) {
		Pair<List<QPNode>, ListMultimap<Table, QPNode>> fields = createClonedFields(ctx);
		return new AndNode(fields.getLeft(), fields.getRight(), ctx.clone(getDateAggregator()));
	}

	@Override
	public boolean isContained() {
		boolean currently = true;
		for (QPNode agg : getChildren()) {
			currently &= agg.isContained();
		}
		return currently;
	}

	public static QPNode of(Collection<? extends QPNode> children, DateAggregationAction action) {
		switch (children.size()) {
			case 0:
				return new Leaf();
			case 1:
				return children.iterator().next();
			default:
				return new AndNode(new ArrayList<>(children), action);
		}
	}

	@Override
	public boolean eventFiltersApply(Bucket bucket, int event) {
		for (QPNode currentTableChild : currentTableChildren) {
			if (!currentTableChild.eventFiltersApply(bucket, event)) {
				return false;
			}
		}
		return true;
	}
}

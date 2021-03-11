package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.queryplan.*;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AndNode extends QPParentNode {

	public AndNode(List<QPNode> children, DateAggregationAction action) {
		super(children, action);
	}

	private AndNode(List<QPNode> children, ListMultimap<TableId, QPNode> childMap, DateAggregator dateAggregator) {
		super(children, childMap, dateAggregator);
	}

	@Override
	public QPNode doClone(CloneContext ctx) {
		Pair<List<QPNode>, ListMultimap<TableId, QPNode>> fields = createClonedFields(ctx);
		return new AndNode(fields.getLeft(), fields.getRight(), getDateAggregator() != null ? ctx.clone(getDateAggregator()): null);
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
}

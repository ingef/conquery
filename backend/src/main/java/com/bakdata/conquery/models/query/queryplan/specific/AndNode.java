package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QPParentNode;

public class AndNode extends QPParentNode {

	public AndNode(List<QPNode> children, DateAggregationAction action) {
		super(children, action);
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
		return switch (children.size()) {
			case 0 -> new Leaf();
			case 1 -> children.iterator().next();
			default -> new AndNode(new ArrayList<>(children), action);
		};
	}
}

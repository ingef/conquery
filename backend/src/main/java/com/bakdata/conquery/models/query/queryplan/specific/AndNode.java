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

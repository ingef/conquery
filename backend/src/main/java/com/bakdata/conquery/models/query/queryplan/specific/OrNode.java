package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.queryplan.DateAggregator;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QPParentNode;
import com.google.common.collect.ListMultimap;

public class OrNode extends QPParentNode {

	public OrNode(List<QPNode> children, DateAggregationAction action) {
		super(children, action);
	}
	
	private OrNode(List<QPNode> children, ListMultimap<Table, QPNode> childMap, DateAggregator dateAggregator) {
		super(children, childMap, dateAggregator);
	}


	@Override
	public boolean isContained() {
		boolean currently = false;
		for(QPNode agg:getChildren()) {
			currently |= agg.isContained();
		}
		return currently;
	}
	
	public static QPNode of(Collection<QPNode> children, DateAggregationAction dateAggregationAction) {
		return switch (children.size()) {
			case 0 -> new Leaf();
			case 1 -> children.iterator().next();
			default -> new OrNode(new ArrayList<>(children), dateAggregationAction);
		};
	}
}

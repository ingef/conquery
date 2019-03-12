package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QPParentNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;

public class OrNode extends QPParentNode {

	public OrNode(List<QPNode> children) {
		super(children);
	}
	
	@Override
	public boolean nextEvent(Block block, int event) {
		boolean currently = false;
		for(QPNode agg:currentTableChildren) {
			currently |= agg.aggregate(block, event);
		}
		return currently;
	}
	
	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		List<QPNode> clones = new ArrayList<>(getChildren());
		clones.replaceAll(qp->qp.clone(plan, clone));
		return new OrNode(clones);
	}
	
	@Override
	public boolean isContained() {
		boolean currently = false;
		for(QPNode agg:getChildren()) {
			currently |= agg.isContained();
		}
		return currently;
	}
	
	public static QPNode of(Collection<QPNode> children) {
		switch (children.size()) {
			case 0:
				return new Leaf();
			case 1:
				return children.iterator().next();
			default:
				return new OrNode(new ArrayList<>(children));
		}
	}
}

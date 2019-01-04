package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.OpenResult;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QPParentNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

public class OrNode extends QPParentNode {

	public OrNode(List<QPNode> children) {
		super(children);
	}
	
	@Override
	protected OpenResult nextEvent(Block block, int event) {
		OpenResult currently = OpenResult.NOT_INCLUDED;
		for(QPNode agg:currentTableChildren) {
			currently = currently.or(agg.aggregate(block, event));
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
		for(QPNode agg:currentTableChildren) {
			currently |= agg.isContained();
		}
		return currently;
	}
}

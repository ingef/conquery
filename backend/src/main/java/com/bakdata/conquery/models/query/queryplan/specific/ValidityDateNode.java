package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

import lombok.ToString;

@ToString
public class ValidityDateNode extends QPChainNode {

	private final Column validityDateColumn;
	
	public ValidityDateNode(Column validityDateColumn, QPNode child) {
		super(child);
		this.validityDateColumn = validityDateColumn;
	}

	@Override
	public boolean nextEvent(Block block, int event) {
		//if validity date is null return
		if(validityDateColumn != null && !block.has(event, validityDateColumn)) {
			return true;
		}
		else {
			return getChild().aggregate(block, event);
		}
	}
	
	@Override
	public boolean isContained() {
		return getChild().isContained();
	}

	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		return new ValidityDateNode(validityDateColumn, getChild().clone(plan, clone));
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		super.nextTable(ctx.withValidityDateColumn(validityDateColumn), currentTable);
	}
}

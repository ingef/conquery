package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import lombok.ToString;

@ToString
public class ValidityDateNode extends QPChainNode {

	private final Column validityDateColumn;
	
	public ValidityDateNode(Column validityDateColumn, QPNode child) {
		super(child);
		this.validityDateColumn = validityDateColumn;
	}

	@Override
	public void nextEvent(Block block, int event) {
		//if validity date is null return
		if(validityDateColumn != null && !block.has(event, validityDateColumn)) {
			return;
		}
		else {
			if(validityDateColumn == null || block.eventIsContainedIn(event, validityDateColumn, context.getDateRestriction())) {
				getChild().nextEvent(block, event);
			}
		}
	}
	
	@Override
	public boolean isContained() {
		return getChild().isContained();
	}

	@Override
	public QPNode doClone(CloneContext ctx) {
		return new ValidityDateNode(validityDateColumn, getChild().clone(ctx));
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		super.nextTable(ctx.withValidityDateColumn(validityDateColumn), currentTable);
	}
}

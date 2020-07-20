package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

public class ValidityDateNode extends QPChainNode {

	private final Column validityDateColumn;
	private transient boolean noRestriction;
	
	public ValidityDateNode(Column validityDateColumn, QPNode child) {
		super(child);
		this.validityDateColumn = validityDateColumn;
	}
	
	@Override
	public void nextEvent(Bucket bucket, int event) {
		//if table without validity columns we continue always
		if(validityDateColumn == null) {
			getChild().nextEvent(bucket, event);
		}

		//if event has null validityDate cancel
		if(!bucket.has(event, validityDateColumn)) {
			return;
		}

		//no dateRestriction or event is in date restriction
		if(noRestriction || bucket.eventIsContainedIn(event, validityDateColumn, context.getDateRestriction())) {
			getChild().nextEvent(bucket, event);
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
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		super.nextTable(ctx.withValidityDateColumn(validityDateColumn), currentTable);
		noRestriction = ctx.getDateRestriction().isAll();
	}
	
	@Override
	public String toString() {
		return "ValidityDateNode [validityDateColumn=" + validityDateColumn + ", getChild()=" + getChild() + "]";
	}
}

package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import java.util.Optional;

public class ValidityDateNode extends QPChainNode {

	private final Column validityDateColumn;
	private transient boolean noRestriction;
	
	public ValidityDateNode(Column validityDateColumn, QPNode child) {
		super(child);
		this.validityDateColumn = validityDateColumn;
	}
	
	@Override
	public void acceptEvent(Bucket bucket, int event) {
		//if table without validity columns we continue always
		if(validityDateColumn == null) {
			getChild().acceptEvent(bucket, event);
			return;
		}

		//if event has null validityDate cancel
		if(!bucket.has(event, validityDateColumn)) {
			return;
		}

		//no dateRestriction or event is in date restriction
		if(noRestriction || bucket.eventIsContainedIn(event, validityDateColumn, context.getDateRestriction())) {
			getChild().acceptEvent(bucket, event);
		}
	}
	
	@Override
	public Optional<Boolean> aggregationFiltersApply() {
		return getChild().aggregationFiltersApply();
	}

	@Override
	public QPNode doClone(CloneContext ctx) {
		return new ValidityDateNode(validityDateColumn, ctx.clone(getChild()));
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		super.nextTable(ctx.withValidityDateColumn(validityDateColumn), currentTable);
		noRestriction = ctx.getDateRestriction().isAll();
	}
	
	@Override
	public String toString() {
		return "ValidityDateNode [validityDateColumn=" + validityDateColumn + ", getChild()=" + getChild() + "]";
	}
}

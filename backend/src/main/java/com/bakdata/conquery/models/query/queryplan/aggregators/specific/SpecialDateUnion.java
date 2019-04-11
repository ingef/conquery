package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

public class SpecialDateUnion implements Aggregator<String> {

	private CDateSet set = CDateSet.create();
	private Column currentColumn;
	private CDateSet dateRestriction;

	@Override
	public void nextTable(QueryContext ctx, Table table) {
		currentColumn = ctx.getValidityDateColumn();
		dateRestriction = ctx.getDateRestriction();
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (currentColumn != null) {
			CDateRange range = block.getAsDateRange(event, currentColumn);
			if(range != null) {
				CDateSet add = CDateSet.create(dateRestriction);
				add.retainAll(CDateSet.create(range));
				set.addAll(add);
				return;
			}
		}
		
		if(dateRestriction.countDays() != null) {
			set.addAll(dateRestriction);
		}
	}

	/**
	 * Helper method to insert dates from outside.
	 * @param other CDateSet to be included.
	 */
	public void merge(CDateSet other){
		set.addAll(other);
	}

	@Override
	public SpecialDateUnion clone(CloneContext ctx) {
		return (SpecialDateUnion) Aggregator.super.clone(ctx);
	}
	
	@Override
	public SpecialDateUnion doClone(CloneContext ctx) {
		return new SpecialDateUnion();
	}

	@Override
	public String getAggregationResult() {
		return set.toString();
	}
	
	public CDateSet getResultSet() {
		return set;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.STRING;
	}
}

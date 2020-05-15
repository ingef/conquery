package com.bakdata.conquery.models.query.queryplan.aggregators.specific.date;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Special Aggregator, used to calculate the times an entity has events after filtering.
 */
public class SpecialDateUnion implements Aggregator<String> {

	private CDateSet set = CDateSet.create();

	@Override
	public void aggregateEvent(Bucket bucket, int event) {
		throw new IllegalStateException("SpecialDateUnion should never be used as a normal aggregator");
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
	
	@Override
	public String toString(){
		return getClass().getSimpleName();
	}
}

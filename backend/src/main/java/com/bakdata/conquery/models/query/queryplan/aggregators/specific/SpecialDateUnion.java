package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import lombok.NonNull;

/**
 * Special Aggregator, used to calculate the times an entity has events after filtering.
 */
public class SpecialDateUnion implements Aggregator<String> {

	private CDateSet set = CDateSet.create();

	private Column currentColumn;
	private @NonNull CDateSet dateRestriction;

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		currentColumn = ctx.getValidityDateColumn();
		dateRestriction = ctx.getDateRestriction();
	}
	
	@Override
	public void aggregateEvent(Bucket bucket, int event) {
		if (currentColumn != null) {
			CDateRange range = bucket.getAsDateRange(event, currentColumn);
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
	
	@Override
	public String toString(){
		return getClass().getSimpleName();
	}
}

package com.bakdata.conquery.models.query.queryplan.aggregators.specific.date;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.UniversalAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.RequiredArgsConstructor;

/**
 * Special Aggregator, used to calculate the times an entity has events after filtering.
 */
@RequiredArgsConstructor
public class SpecialDateUnion implements UniversalAggregator<String> {

	private CDateSet set = CDateSet.create();

	private Column currentColumn;
	private CDateSet dateRestriction;


	@Override
	public void nextTable(QueryExecutionContext ctx, TableId table) {
		currentColumn = ctx.getValidityDateColumn();
		dateRestriction = ctx.getDateRestriction();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (currentColumn != null) {
			CDateRange range = bucket.getAsDateRange(event, currentColumn);
			if(range != null) {
				CDateSet add = CDateSet.create(dateRestriction);
				add.retainAll(CDateSet.create(range));
				getResultSet().addAll(add);
				return;
			}
		}

		if(!dateRestriction.isEmpty()) {
			getResultSet().addAll(dateRestriction);
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

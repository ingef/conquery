package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.common.BitMapCDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.util.QueryUtils;
import lombok.RequiredArgsConstructor;

/**
 * Special Aggregator, used to calculate the times an entity has events after filtering.
 */
@RequiredArgsConstructor
public class SpecialDateUnion implements Aggregator<String> {


	private final BitMapCDateSet set = QueryUtils.createPreAllocatedDateSet();

	private Column currentColumn;

	private BitMapCDateSet dateRestriction;

	@Override
	public void nextTable(QueryExecutionContext ctx, TableId table) {
		currentColumn = ctx.getValidityDateColumn();
		dateRestriction = ctx.getDateRestriction();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (currentColumn == null) {
			if(!dateRestriction.isEmpty()) {
				getResultSet().addAll(dateRestriction);
			}
			return;
		}

		CDateRange range = bucket.getAsDateRange(event, currentColumn);

		if (range == null) {
			return;
		}

		set.maskedAdd(range, dateRestriction);
	}

	/**
	 * Helper method to insert dates from outside.
	 * @param other ICDateSet to be included.
	 */
	public void merge(BitMapCDateSet other){
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
	
	public BitMapCDateSet getResultSet() {
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

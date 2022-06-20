package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Special Aggregator, used to calculate the times an entity has events after filtering.
 */
@RequiredArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class SpecialDateUnion extends Aggregator<CDateSet> {

	private CDateSet set = CDateSet.create();

	private Column currentColumn;
	private CDateSet dateRestriction;


	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		set.clear();
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table table) {
		currentColumn = ctx.getValidityDateColumn();
		dateRestriction = ctx.getDateRestriction();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (currentColumn != null && bucket.has(event, currentColumn)) {
			set.maskedAdd(bucket.getAsDateRange(event, currentColumn), dateRestriction);
			return;
		}

		if(!dateRestriction.isEmpty()) {
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
	public CDateSet createAggregationResult() {
		return CDateSet.create(set.asRanges());
	}

	@Override
	public ResultType getResultType() {
		return new ResultType.ListT(ResultType.DateRangeT.INSTANCE);
	}
}

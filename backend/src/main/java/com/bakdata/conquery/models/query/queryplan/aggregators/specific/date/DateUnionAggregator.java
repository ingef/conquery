package com.bakdata.conquery.models.query.queryplan.aggregators.specific.date;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.Getter;

/**
 * Aggregator, listing all days present.
 */
public class DateUnionAggregator implements Aggregator<String> {

	private CDateSet set = CDateSet.create();
	private CDateSet dateRestriction;

	@Getter
	private Column column;

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		dateRestriction = ctx.getDateRestriction();
		column = ctx.getValidityDateColumn();

		if(!column.getType().isDateCompatible()){
			throw new IllegalArgumentException("ValidityDate can only be Date based.");
		}
	}

	@Override
	public void aggregateEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		//otherwise the result would be something weird
		if(bucket.getAsDateRange(event, getColumn()).isOpen()) {
			return;
		}

		CDateSet range = CDateSet.create();
		range.add(bucket.getAsDateRange(event, getColumn()));

		range.retainAll(dateRestriction);

		set.addAll(range);
	}

	@Override
	public DateUnionAggregator doClone(CloneContext ctx) {
		return new DateUnionAggregator();
	}

	@Override
	public String getAggregationResult() {
		return set.toString();
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.STRING;
	}
}

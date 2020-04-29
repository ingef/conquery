package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EventDateUnionAggregator implements Aggregator<String>{

	private final Set<TableId> requiredTables;
	private Column validityDateColumn;
	private CDateSet set = CDateSet.create();
	private CDateSet dateRestriction;
	

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		requiredTables.addAll(this.requiredTables);
	}
	
	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		validityDateColumn = ctx.getValidityDateColumn();
		dateRestriction = ctx.getDateRestriction();
		Aggregator.super.nextTable(ctx, currentTable);
	}

	@Override
	public Aggregator<String> doClone(CloneContext ctx) {
		return new EventDateUnionAggregator(requiredTables);
	}

	@Override
	public String getAggregationResult() {
		set.retainAll(dateRestriction);
		return set.toString();
	}

	@Override
	public void aggregateEvent(Bucket bucket, int event) {
		if (!bucket.has(event, validityDateColumn)) {
			return;
		}
		set.add(bucket.getAsDateRange(event, validityDateColumn));
	}

	@Override
	public ResultType getResultType() {
		return ResultType.STRING;
	}

}

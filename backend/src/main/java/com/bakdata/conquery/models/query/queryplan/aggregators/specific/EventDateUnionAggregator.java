package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.Objects;
import java.util.Set;

import com.bakdata.conquery.models.common.BitMapCDateSet;
import com.bakdata.conquery.models.common.CDateSetCache;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.RequiredArgsConstructor;

/**
 * Collects the event dates of all events that are applicable to the specific
 * part of a query. Eventually the set of collected dates is tailored to the
 * provided date restriction.
 *
 */
@RequiredArgsConstructor
public class EventDateUnionAggregator implements Aggregator<String> {

	private BitMapCDateSet set = CDateSetCache.createPreAllocatedDateSet();
	private final Set<TableId> requiredTables;
	private Column validityDateColumn;

	private BitMapCDateSet dateRestriction;

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		requiredTables.addAll(this.requiredTables);
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		validityDateColumn = Objects.requireNonNull(ctx.getValidityDateColumn());
		if (!validityDateColumn.getType().isDateCompatible()) {
			throw new IllegalStateException("The validityDateColumn " + validityDateColumn + " is not a DATE TYPE");
		}
		
		dateRestriction = ctx.getDateRestriction();
		Aggregator.super.nextTable(ctx, currentTable);
	}

	@Override
	public Aggregator<String> doClone(CloneContext ctx) {
		return new EventDateUnionAggregator(requiredTables);
	}

	@Override
	public String getAggregationResult() {
		return set.toString();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, validityDateColumn)) {
			return;
		}

		set.maskedAdd(bucket.getAsDateRange(event, validityDateColumn), dateRestriction);
	}

	@Override
	public ResultType getResultType() {
		return ResultType.STRING;
	}

}

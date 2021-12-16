package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Collects the event dates of all events that are applicable to the specific
 * part of a query. Eventually the set of collected dates is tailored to the
 * provided date restriction.
 *
 */
@RequiredArgsConstructor
@ToString(of = {"requiredTables"})
public class EventDateUnionAggregator extends Aggregator<CDateSet> {

	private final Set<Table> requiredTables;
	private Column validityDateColumn;
	private CDateSet set = CDateSet.create();
	private CDateSet dateRestriction;

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		requiredTables.addAll(this.requiredTables);
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		set.clear();
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		validityDateColumn = ctx.getValidityDateColumn();
		if (validityDateColumn != null && !validityDateColumn.getType().isDateCompatible()) {
			throw new IllegalStateException("The validityDateColumn " + validityDateColumn + " is not a DATE TYPE");
		}
		
		dateRestriction = ctx.getDateRestriction();
		super.nextTable(ctx, currentTable);
	}

	@Override
	public CDateSet createAggregationResult() {
		return CDateSet.create(set.asRanges());
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if(validityDateColumn == null) {
			set.addAll(dateRestriction);
			return;
		}

		if (!bucket.has(event, validityDateColumn)) {
			return;
		}
		set.maskedAdd(bucket.getAsDateRange(event, validityDateColumn), dateRestriction);
	}

	@Override
	public ResultType getResultType() {
		return new ResultType.ListT(ResultType.DateRangeT.INSTANCE);
	}

}

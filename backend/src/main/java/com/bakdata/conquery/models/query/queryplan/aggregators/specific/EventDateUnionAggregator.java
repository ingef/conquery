package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.types.ResultType;
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
	private ValidityDate validityDateColumn;
	private CDateSet set = CDateSet.createEmpty();
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

		final CDateRange dateRange = validityDateColumn.getValidityDate(event, bucket);

		if (dateRange == null){
			return;
		}

		set.maskedAdd(dateRange, dateRestriction);
	}

	@Override
	public ResultType getResultType() {
		return new ResultType.ListT(ResultType.DateRangeT.INSTANCE);
	}

}

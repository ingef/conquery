package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.Collections;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;

/**
 * Collects the event dates of all events that are applicable to the specific
 * part of a query. Eventually the set of collected dates is tailored to the
 * provided date restriction.
 *
 */
@Data
@ToString(of = {"requiredTables"})
public class EventDateUnionAggregator extends Aggregator<CDateSet> {

	@NonNull
	private Set<Table> requiredTables = Collections.emptySet();
	/**
	 * If present, dates are only produced if it's also contained.
	 */
	private QPNode owner;
	private final CDateSet set = CDateSet.createEmpty();

	private ValidityDate validityDateColumn;
	private CDateSet dateRestriction;

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		requiredTables.addAll(getRequiredTables());
	}

	public void setOwner(QPNode owner) {
		this.owner = owner;
		// We have to preemptively collect required tables and store them, as otherwise this causes a recursive loop
		setRequiredTables(owner.collectRequiredTables());
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
		if (owner != null && !owner.isContained()) {
			return CDateSet.createEmpty();
		}

		return CDateSet.create(set.asRanges());
	}

	@Override
	public void consumeEvent(Bucket bucket, int event) {
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

}

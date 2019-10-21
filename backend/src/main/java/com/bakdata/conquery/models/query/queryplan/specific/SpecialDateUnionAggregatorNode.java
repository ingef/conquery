package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

public class SpecialDateUnionAggregatorNode extends QPNode {

	private final TableId requiredTable;
	private final SpecialDateUnion aggregator;
	private boolean triggered = false;
	
	private Column currentColumn;
	private CDateSet dateRestriction;
	
	public SpecialDateUnionAggregatorNode(TableId requiredTable, SpecialDateUnion aggregator) {
		this.aggregator = aggregator;
		this.requiredTable = requiredTable;
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		requiredTables.add(requiredTable);
	}
	
	@Override
	public SpecialDateUnionAggregatorNode doClone(CloneContext ctx) {
		SpecialDateUnion aggClone = aggregator.clone(ctx);
		return new SpecialDateUnionAggregatorNode(requiredTable, aggClone);
	}
	
	@Override
	public void nextTable(QueryContext ctx, Table table) {
		currentColumn = ctx.getValidityDateColumn();
		dateRestriction = ctx.getDateRestriction();
	}
	
	
	
	@Override
	public void nextEvent(Bucket bucket, int event) {
		triggered = true;
		if (currentColumn != null) {
			CDateRange range = bucket.getAsDateRange(event, currentColumn);
			if(range != null) {
				CDateSet add = CDateSet.create(dateRestriction);
				add.retainAll(CDateSet.create(range));
				aggregator.getResultSet().addAll(add);
				return;
			}
		}
		
		if(dateRestriction.countDays() != null) {
			aggregator.getResultSet().addAll(dateRestriction);
		}
	}

	@Override
	public boolean isContained() {
		return triggered;
	}
	
	@Override
	public boolean isOfInterest(Bucket bucket) {
		return true;
	}
	
	@Override
	public boolean isOfInterest(Entity entity) {
		return true;
	}

	@Override
	public void reset() {
		aggregator.reset();
		triggered = false;
		
		dateRestriction.clear();
		currentColumn = null;
		
	}
}

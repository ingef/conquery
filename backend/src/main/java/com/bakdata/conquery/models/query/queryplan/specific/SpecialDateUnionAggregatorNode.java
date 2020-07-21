package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

public class SpecialDateUnionAggregatorNode extends AggregatorNode<String> {

	private TableId requiredTable;

	private Column currentColumn;
	private CDateSet dateRestriction;
	
	public SpecialDateUnionAggregatorNode(TableId requiredTable, SpecialDateUnion aggregator) {
		super(aggregator);
		this.requiredTable = requiredTable;
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		requiredTables.add(requiredTable);
	}
	
	@Override
	public SpecialDateUnionAggregatorNode doClone(CloneContext ctx) {
		return new SpecialDateUnionAggregatorNode(requiredTable, (SpecialDateUnion) getAggregator().clone(ctx));
	}
	
	@Override
	public void nextTable(QueryExecutionContext ctx, Table table) {
		currentColumn = ctx.getValidityDateColumn();
		dateRestriction = ctx.getDateRestriction();
	}
	
	
	
	@Override
	public void acceptEvent(Bucket bucket, int event) {
		setTriggered(true);
		if (currentColumn != null) {
			CDateRange range = bucket.getAsDateRange(event, currentColumn);
			if(range != null) {
				CDateSet add = CDateSet.create(dateRestriction);
				add.retainAll(CDateSet.create(range));
				((SpecialDateUnion) getAggregator()).getResultSet().addAll(add);
				return;
			}
		}
		
		if(dateRestriction.countDays() != null) {
			((SpecialDateUnion) getAggregator()).getResultSet().addAll(dateRestriction);
		}
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		return true;
	}
	
	@Override
	public boolean isOfInterest(Entity entity) {
		return true;
	}
}

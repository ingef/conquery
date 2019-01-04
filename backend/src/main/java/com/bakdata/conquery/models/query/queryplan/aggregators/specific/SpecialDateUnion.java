package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;

public class SpecialDateUnion implements Aggregator<CDateSet> {

	private CDateSet set = CDateSet.create();
	private Column currentColumn;
	private CDateSet dateRestriction;

	@Override
	public void nextTable(QueryContext ctx, Table table) {
		currentColumn = ctx.getValidityDateColumn();
		dateRestriction = ctx.getDateRestriction();
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (currentColumn != null) {
			CDateRange range = block.getAsDateRange(event, currentColumn);
			if(range != null) {
				CDateSet add = CDateSet.create(dateRestriction);
				add.retainAll(CDateSet.create(range));
				set.addAll(add);
				return;
			}
		}
		
		set.addAll(dateRestriction);
	}

	@Override
	public SpecialDateUnion clone() {
		return new SpecialDateUnion();
	}

	@Override
	public CDateSet getAggregationResult() {
		return set;
	}
}

package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(of = "restriction", callSuper = true)
public class DateRestrictingNode extends QPChainNode {

	protected final CDateSet restriction;

	public DateRestrictingNode(CDateSet restriction, QPNode child) {
		super(child);
		this.restriction = restriction;
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		//if there was no date restriction we can just use the restriction CDateSet
		if (ctx.getDateRestriction().isAll()) {
			ctx = ctx.withDateRestriction(CDateSet.create(restriction));
		}
		else {
			final CDateSet dateRestriction = CDateSet.create(ctx.getDateRestriction());
			dateRestriction.retainAll(restriction);
			ctx = ctx.withDateRestriction(dateRestriction);
		}
		super.nextTable(ctx, currentTable);
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		getChild().acceptEvent(bucket, event);
	}

	@Override
	public boolean isContained() {
		return getChild().isContained();
	}

}

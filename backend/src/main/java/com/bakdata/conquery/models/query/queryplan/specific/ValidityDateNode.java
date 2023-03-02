package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Map;
import java.util.Objects;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import lombok.ToString;

@ToString(of = "validityDateColumn", callSuper = true)
public class ValidityDateNode extends QPChainNode {

	private final Column validityDateColumn;
	private transient CDateSet restriction;

	protected Map<Bucket, CBlock> preCurrentRow;

	public ValidityDateNode(Column validityDateColumn, QPNode child) {
		super(child);
		this.validityDateColumn = validityDateColumn;
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {

		//if event has null validityDate cancel
		if (!bucket.has(event, validityDateColumn)) {
			return;
		}

		//no dateRestriction or event is in date restriction
		if (restriction.isAll() || bucket.eventIsContainedIn(event, validityDateColumn, context.getDateRestriction())) {
			getChild().acceptEvent(bucket, event);
		}
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		final CBlock cBlock = Objects.requireNonNull(preCurrentRow.get(bucket));

		final CDateRange range = cBlock.getEntityDateRange(entity.getId());

		return restriction.intersects(range) && super.isOfInterest(bucket);
	}

	@Override
	public boolean isContained() {
		return getChild().isContained();
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		super.nextTable(ctx.withValidityDateColumn(validityDateColumn), currentTable);
		restriction = ctx.getDateRestriction();

		preCurrentRow = ctx.getBucketManager().getEntityCBlocksForConnector(getEntity(), context.getConnector());
	}
}

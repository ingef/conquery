package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Map;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.google.common.base.Preconditions;
import lombok.ToString;

@ToString(of = "validityDate", callSuper = true)
public class ValidityDateNode extends QPChainNode {

	private final ValidityDate validityDate;
	protected Map<BucketId, CBlockId> preCurrentRow;
	private transient CDateSet restriction;

	public ValidityDateNode(ValidityDate validityDate, QPNode child) {
		super(child);
		Preconditions.checkNotNull(validityDate, this.getClass().getSimpleName() + " needs a validityDate");
		this.validityDate = validityDate;
	}

	@Override
	public boolean acceptEvent(Bucket bucket, int event) {
		//no dateRestriction or event is in date restriction
		final boolean contained = bucket.eventIsContainedIn(event, validityDate, context.getDateRestriction());

		if (!contained){
			return false;
		}

		return getChild().acceptEvent(bucket, event);
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		super.nextTable(ctx.withValidityDateColumn(validityDate), currentTable);
		restriction = ctx.getDateRestriction();

		preCurrentRow = ctx.getBucketManager().getEntityCBlocksForConnector(getEntity(), context.getConnector().getId());
	}

	@Override
	public boolean isContained() {
		return getChild().isContained();
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		final CBlock cBlock = preCurrentRow.get(bucket.getId()).resolve();

		final CDateRange range = cBlock.getEntityDateRange(entity.getId());

		return restriction.intersects(range) && super.isOfInterest(bucket);
	}
}

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
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(of = "restriction", callSuper = true)
public class DateRestrictingNode extends QPChainNode {

	protected final CDateSet restriction;
	protected Column validityDateColumn;
	protected Map<Bucket, CBlock> preCurrentRow = null;

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
			CDateSet dateRestriction = CDateSet.create(ctx.getDateRestriction());
			dateRestriction.retainAll(restriction);
			ctx = ctx.withDateRestriction(dateRestriction);
		}
		super.nextTable(ctx, currentTable);


		preCurrentRow = ctx.getBucketManager().getEntityCBlocksForConnector(getEntity(), context.getConnector());
		validityDateColumn = context.getValidityDateColumn();

		if (validityDateColumn != null && !validityDateColumn.getType().isDateCompatible()) {
			throw new IllegalStateException("The validityDateColumn " + validityDateColumn + " is not a DATE TYPE");
		}
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		CBlock cBlock = Objects.requireNonNull(preCurrentRow.get(bucket));

		if (validityDateColumn == null) {
			// If there is no validity date set for a concept there is nothing to restrict
			return super.isOfInterest(bucket);
		}

		CDateRange range = cBlock.getEntityDateRange(entity.getId());

		return restriction.intersects(range) && super.isOfInterest(bucket);
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (validityDateColumn != null && !bucket.eventIsContainedIn(event, validityDateColumn, restriction)) {
			return;
		}
		getChild().acceptEvent(bucket, event);
	}

	@Override
	public boolean isContained() {
		return getChild().isContained();
	}

}

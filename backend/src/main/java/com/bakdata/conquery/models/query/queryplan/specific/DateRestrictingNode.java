package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Map;
import java.util.Objects;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DateRestrictingNode extends QPChainNode {

	protected final CDateSet restriction;
	protected Column validityDateColumn;
	protected Map<BucketId, CBlock> preCurrentRow = null;

	public DateRestrictingNode(CDateSet restriction, QPNode child) {
		super(child);
		this.restriction = restriction;
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		//if there was no date restriction we can just use the restriction CDateSet
		if(ctx.getDateRestriction().isAll()) {
			ctx = ctx.withDateRestriction(CDateSet.create(restriction));
		}
		else {
			CDateSet dateRestriction = CDateSet.create(ctx.getDateRestriction());
			dateRestriction.retainAll(restriction);
			ctx = ctx.withDateRestriction(dateRestriction);
		}
		super.nextTable(ctx, currentTable);


		preCurrentRow = ctx.getBucketManager().getEntityCBlocksForConnector(getEntity(), context.getConnector().getId());
		validityDateColumn = context.getValidityDateColumn();

		if (validityDateColumn != null && !validityDateColumn.getType().isDateCompatible()) {
			throw new IllegalStateException("The validityDateColumn " + validityDateColumn + " is not a DATE TYPE");
		}
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		CBlock cBlock = Objects.requireNonNull(preCurrentRow.get(bucket.getId()));

		if (validityDateColumn == null) {
			// If there is no validity date set for a concept there is nothing to restrict
			return true;
		}

		int entityId = entity.getId();

		// Entity has no date-columns.
		if(!cBlock.getMinDate().containsKey(entityId) && !cBlock.getMaxDate().containsKey(entityId)){
			return super.isOfInterest(bucket);
		}

		final int min = cBlock.getMinDate().getOrDefault(entityId, Integer.MIN_VALUE);
		final int max = cBlock.getMaxDate().getOrDefault(entityId, Integer.MAX_VALUE);

		CDateRange range = CDateRange.of(min, max);

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

	@Override
	public QPNode doClone(CloneContext ctx) {
		return new DateRestrictingNode(restriction, ctx.clone(getChild()));
	}
}

package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.Getter;

@Getter
public class ConceptNode extends QPChainNode {

	private final ConceptElement<?>[] concepts;
	private final long requiredBits;
	private final Connector resolvedConnector;
	private final SecondaryIdId selectedSecondaryId;
	private boolean tableActive = false;
	private Map<BucketId, CBlock> preCurrentRow = null;
	private CBlock currentRow = null;

	public ConceptNode(ConceptElement[] concepts, long requiredBits, QPNode child, Connector resolvedConnector, SecondaryIdId selectedSecondaryId) {
		super(child);
		this.concepts = concepts;
		this.requiredBits = requiredBits;

		this.resolvedConnector = resolvedConnector;

		this.selectedSecondaryId = selectedSecondaryId;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		super.init(entity, context);
		preCurrentRow = context.getBucketManager().getEntityCBlocksForConnector(getEntity(), getResolvedConnector().getId());
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		tableActive = getResolvedConnector().getTable().getId().equals(currentTable)
					  && ctx.getActiveSecondaryId() == selectedSecondaryId;

		if (tableActive) {
			super.nextTable(ctx.withConnector(getResolvedConnector()), currentTable);
		}
	}

	@Override
	public void nextBlock(Bucket bucket) {
		if (tableActive) {
			currentRow = Objects.requireNonNull(preCurrentRow.get(bucket.getId()));
			super.nextBlock(bucket);
		}
	}


	@Override
	public boolean isOfInterest(Entity entity) {
		return context.getBucketManager().hasEntityCBlocksForConnector(entity, getResolvedConnector().getId());
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		if (!tableActive) {
			return false;
		}

		CBlock row = Objects.requireNonNull(preCurrentRow.get(bucket.getId()));

		int localEntity = bucket.toLocal(entity.getId());
		long bits = row.getIncludedConcepts()[localEntity];

		if ((bits & requiredBits) != 0L || requiredBits == 0L) {
			return super.isOfInterest(bucket);
		}
		return false;
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!tableActive) {
			return;
		}

		//check concepts
		int[] mostSpecificChildren;
		if (currentRow.getMostSpecificChildren() != null
			&& ((mostSpecificChildren = currentRow.getMostSpecificChildren()[event]) != null)) {

			for (ConceptElement<?> ce : concepts) { //see #177  we could improve this by building a a prefix tree over concepts.prefix
				if (ce.matchesPrefix(mostSpecificChildren)) {
					getChild().acceptEvent(bucket, event);
				}
			}
		}
		else {
			for (ConceptElement ce : concepts) { //see #178  we could improve this by building a a prefix tree over concepts.prefix
				if (ce.getConcept() == ce) {
					getChild().acceptEvent(bucket, event);
				}
			}
		}
	}

	@Override
	public boolean isContained() {
		return getChild().isContained();
	}

	@Override
	public QPNode doClone(CloneContext ctx) {
		return new ConceptNode(concepts, requiredBits, ctx.clone(getChild()), getResolvedConnector(), selectedSecondaryId);
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		super.collectRequiredTables(requiredTables);
		requiredTables.add(getResolvedConnector().getTable().getId());
	}
}

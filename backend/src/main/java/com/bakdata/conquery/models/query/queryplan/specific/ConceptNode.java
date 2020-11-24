package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.Getter;

@Getter
public class ConceptNode extends QPChainNode {

	private final ConceptElement<?>[] concepts;
	private final long requiredBits;
	private final CQTable table;
	private final Set<SecondaryId> usedSecondaryIds;
	private boolean tableActive = false;
	private Map<BucketId, CBlock> preCurrentRow = null;
	private CBlock currentRow = null;
	private boolean excludeFromSecondaryIdQuery;

	public ConceptNode(ConceptElement[] concepts, long requiredBits, CQTable table, QPNode child, boolean excludeFromSecondaryIdQuery) {
		super(child);
		this.concepts = concepts;
		this.requiredBits = requiredBits;
		this.table = table;
		this.excludeFromSecondaryIdQuery = excludeFromSecondaryIdQuery;

		usedSecondaryIds = Arrays.stream(getTable().getResolvedConnector().getTable().getColumns())
								 .map(Column::getSecondaryId)
								 .filter(Objects::nonNull)
								 .collect(Collectors.toSet());
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		super.init(entity, context);
		preCurrentRow = context.getBucketManager().getEntityCBlocksForConnector(getEntity(), table.getId());
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		tableActive = table.getResolvedConnector().getTable().getId().equals(currentTable);

		// deactivate us, if we are in a SecondaryIdQuery, and want to be excluded.
		if (excludeFromSecondaryIdQuery && ctx.getSecondaryIdQueryPlanPhase() == QueryExecutionContext.SecondaryIdQueryPlanPhase.WithId) {
			tableActive = false;
		}
		// Deactivate us for table if we are in SecondaryId-Query, but not in phase for SecondaryIds.
		if (!excludeFromSecondaryIdQuery
			&& ctx.getSecondaryIdQueryPlanPhase() == QueryExecutionContext.SecondaryIdQueryPlanPhase.WithoutId
			&& usedSecondaryIds.contains(ctx.getActiveSecondaryId())) {
			tableActive = false;
		}

		if (tableActive) {
			super.nextTable(ctx.withConnector(table.getResolvedConnector()), currentTable);
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
		return context.getBucketManager().hasEntityCBlocksForConnector(entity, table.getId());
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
		return new ConceptNode(concepts, requiredBits, table, ctx.clone(getChild()), excludeFromSecondaryIdQuery);
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		super.collectRequiredTables(requiredTables);
		requiredTables.add(table.getResolvedConnector().getTable().getId());
	}
}

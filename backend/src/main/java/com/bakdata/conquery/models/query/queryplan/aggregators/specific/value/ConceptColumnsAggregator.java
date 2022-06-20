package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.types.ResultType;
import com.google.common.collect.ImmutableSet;
import lombok.ToString;

@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class ConceptColumnsAggregator extends Aggregator<Set<int[]>> {

	private final Set<int[]> entries = new HashSet<>();
	private final TreeConcept concept;

	private Column column;
	private Entity entity;
	private Map<Bucket, CBlock> cblocks;
	private CBlock cblock;

	public ConceptColumnsAggregator(TreeConcept concept) {
		super();
		this.concept = concept;
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		for (ConceptTreeConnector connector : concept.getConnectors()) {
			if (connector.getTable().equals(currentTable)) {
				column = connector.getColumn();
				cblocks = ctx.getBucketManager().getEntityCBlocksForConnector(entity, connector);
				break;
			}
		}


	}

	@Override
	public void nextBlock(Bucket bucket) {
		this.cblock = cblocks.get(bucket);
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		this.entity = entity;
		entries.clear();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (bucket.has(event, column)) {
			final int[] mostSpecificChild = cblock.getEventMostSpecificChild(event);
			final ConceptTreeNode<?> element = concept.getElementByLocalId(mostSpecificChild);

			entries.add(mostSpecificChild);

		}
	}

	@Override
	public Set<int[]> createAggregationResult() {
		return entries.isEmpty() ? null : ImmutableSet.copyOf(entries);
	}


	@Override
	public ResultType getResultType() {
		throw new IllegalStateException();
	}
}

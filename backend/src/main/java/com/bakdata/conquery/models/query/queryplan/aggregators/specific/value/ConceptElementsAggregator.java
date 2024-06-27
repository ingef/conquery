package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.ToString;

@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class ConceptElementsAggregator extends Aggregator<Set<Integer>> {

	private final IntSet entries = new IntOpenHashSet();

	private Column column;
	private Entity entity;
	private Map<BucketId, CBlockId> cblocks;
	private CBlock cblock;

	private final Map<Table, Connector> tableConnectors;

	public ConceptElementsAggregator(TreeConcept concept) {
		super();
		tableConnectors = concept.getConnectors().stream()
								 .filter(conn -> conn.getColumn() != null)
								 .collect(Collectors.toMap(Connector::getResolvedTable, Functions.identity()));
	}

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		requiredTables.addAll(tableConnectors.keySet());
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		final Connector connector = tableConnectors.get(currentTable);

		if (connector == null) {
			column = null;
			return;
		}

		final ColumnId columnId = connector.getColumn();
		column = columnId != null ? columnId.resolve() : null;
		cblocks = ctx.getBucketManager().getEntityCBlocksForConnector(entity, connector.getId());
	}

	@Override
	public void nextBlock(Bucket bucket) {
		cblock = cblocks.get(bucket.getId()).resolve();
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		this.entity = entity;
		entries.clear();
	}

	@Override
	public void consumeEvent(Bucket bucket, int event) {
		if (!bucket.has(event, column)) {
			return;
		}

		final int mostSpecificChild = cblock.getMostSpecificChildLocalId(event);
		entries.add(mostSpecificChild);
	}

	@Override
	public Set<Integer> createAggregationResult() {
		return entries.isEmpty() ? null : ImmutableSet.copyOf(entries);
	}


}

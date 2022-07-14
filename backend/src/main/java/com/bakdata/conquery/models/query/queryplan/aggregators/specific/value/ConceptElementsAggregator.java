package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.types.ResultType;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.ToString;

@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class ConceptElementsAggregator extends Aggregator<Set<Integer>> {

	private final IntSet entries = new IntOpenHashSet();
	private final TreeConcept concept;

	private Column column;
	private Entity entity;
	private Map<Bucket, CBlock> cblocks;
	private CBlock cblock;

	public ConceptElementsAggregator(TreeConcept concept) {
		super();
		this.concept = concept;
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		Optional<ConceptTreeConnector> maybeConnector =
				concept.getConnectors().stream()
					   .filter(conn -> conn.getTable().equals(currentTable))
					   .findAny();

		if (maybeConnector.isEmpty()) {
			cblocks = null;
			column = null;
			return;
		}

		final ConceptTreeConnector connector = maybeConnector.get();

		column = connector.getColumn();
		cblocks = ctx.getBucketManager().getEntityCBlocksForConnector(entity, connector);
	}

	@Override
	public void nextBlock(Bucket bucket) {
		cblock = cblocks.get(bucket);
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		this.entity = entity;
		entries.clear();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
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


	@Override
	public ResultType getResultType() {
		return new ResultType.ListT(new ResultType.StringT((val, settings) -> TableExportQuery.printValue(concept, val, settings)));
	}
}

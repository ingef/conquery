package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(of = {"table", "selectedSecondaryId"}, callSuper = true)
public class ConceptNode extends QPChainNode {

	private final List<ConceptElement<?>> concepts;
	private final long requiredBits;
	private final CQTable table;
	private final SecondaryIdDescription selectedSecondaryId;
	private boolean tableActive;
	private Map<Bucket, CBlock> preCurrentRow;
	private CBlock currentRow;

	public ConceptNode(QPNode child, List<ConceptElement<?>> concepts, CQTable table, SecondaryIdDescription selectedSecondaryId) {
		this(child, concepts, calculateBitMask(concepts), table, selectedSecondaryId);
	}

	// For cloning
	private ConceptNode(QPNode child, List<ConceptElement<?>> concepts, long requiredBits, CQTable table, SecondaryIdDescription selectedSecondaryId) {
		super(child);
		this.concepts = concepts;
		this.requiredBits =	requiredBits;
		this.table = table;

		this.selectedSecondaryId = selectedSecondaryId;
	}

	/**
	 * Calculate the bitmask for the supplied {@link ConceptElement}s which is eventually compared with the
	 * the bitmasks of each entity. (See {@link CBlock#getIncludedConceptElementsPerEntity()})
	 */
	public static long calculateBitMask(Collection<ConceptElement<?>> concepts) {
		long mask = 0;
		for (ConceptElement<?> concept : concepts) {
			final int[] prefix = concept.getPrefix();
			mask |= CBlock.calculateBitMask(prefix.length - 1, prefix);
		}
		return mask;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		super.init(entity, context);
		preCurrentRow = context.getBucketManager().getEntityCBlocksForConnector(getEntity(),table.getConnector());
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		tableActive = table.getConnector().getTable().equals(currentTable)
					  && ctx.getActiveSecondaryId() == selectedSecondaryId;
		if(tableActive) {
			super.nextTable(ctx.withConnector(table.getConnector()), currentTable);
		}
	}

	@Override
	public void nextBlock(Bucket bucket) {
		if (tableActive) {
			currentRow = Objects.requireNonNull(preCurrentRow.get(bucket));
			super.nextBlock(bucket);
		}
	}


	@Override
	public boolean isOfInterest(Entity entity) {
		return context.getBucketManager().hasEntityCBlocksForConnector(entity, table.getConnector())
			   && getChild().isOfInterest(entity);
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		if (!tableActive) {
			return false;
		}

		final CBlock cBlock = Objects.requireNonNull(preCurrentRow.get(bucket));

		if(cBlock.isConceptIncluded(entity.getId(), requiredBits)) {
			return super.isOfInterest(bucket);
		}
		return false;
	}

	@Override
	public boolean acceptEvent(Bucket bucket, int event) {
		if (!tableActive) {
			return false;
		}

		//check concepts
		final int[] mostSpecificChildren = currentRow.getPathToMostSpecificChild(event);

		boolean consumed = false;

		for (ConceptElement<?> ce : concepts) {
			if ((mostSpecificChildren != null && ce.matchesPrefix(mostSpecificChildren)) || ce.getConcept() == ce) {
				consumed |= getChild().acceptEvent(bucket, event);
			}
		}

		return consumed;
	}

	@Override
	public boolean isContained() {
		return getChild().isContained();
	}

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		super.collectRequiredTables(requiredTables);
		requiredTables.add(table.getConnector().getTable());
	}

}

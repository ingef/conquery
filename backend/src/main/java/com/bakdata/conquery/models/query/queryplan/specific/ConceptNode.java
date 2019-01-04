package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.entity.EntityRow;
import com.bakdata.conquery.models.query.queryplan.OpenResult;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.google.common.collect.Multiset;

public class ConceptNode extends QPChainNode {

	private final ConceptElement[] concepts;
	private final CQTable table;
	private final Column validityDateColumn;
	private boolean active = false;
	private EntityRow currentRow = null;
	
	public ConceptNode(ConceptElement[] concepts, CQTable table, Column validityDateColumn, QPNode child) {
		super(child);
		this.concepts = concepts;
		this.table = table;
		this.validityDateColumn = validityDateColumn;
	}

	@Override
	public void nextBlock(Block block) {
		super.nextBlock(block);
		currentRow = entity.getCBlocks().get(table.getResolvedConnector(), block);
	}

	@Override
	protected OpenResult nextEvent(Block block, int event) {
		if (active) {

			int[] mostSpecificChildren;

			if (currentRow.getCBlock().getMostSpecificChildren() != null
				&& ((mostSpecificChildren = currentRow.getCBlock().getMostSpecificChildren().get(event)) != null)) {

				for (ConceptElement ce : concepts) { //TODO we could improve this by building a a prefix tree over concepts.prefix
					if (ce.matchesPrefix(mostSpecificChildren)) {
						return getChild().aggregate(block, event);
					}
				}
			}
			else {
				for (ConceptElement ce : concepts) { //TODO we could improve this by building a a prefix tree over concepts.prefix
					if (ce.getConcept() == ce) {
						return getChild().aggregate(block, event);
					}
				}
			}
		}
		return OpenResult.MAYBE;
	}
	
	@Override
	public boolean isContained() {
		return getChild().isContained();
	}

	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		return new ConceptNode(concepts, table, validityDateColumn, getChild().clone(plan, clone));
	}

	@Override
	public Multiset<Table> collectRequiredTables() {
		Multiset<Table> result = getChild().collectRequiredTables();
		result.add(table.getResolvedConnector().getTable());
		return result;
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		active = table.getResolvedConnector().getTable().equals(currentTable);
		if(active)
			super.nextTable(ctx.withValidityDateColumn(validityDateColumn), currentTable);
	}
}

package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Set;

import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.entity.EntityRow;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

public class ConceptNode extends QPChainNode {

	private final ConceptElement[] concepts;
	private final CQTable table;
	private boolean active = false;
	private EntityRow currentRow = null;
	
	public ConceptNode(ConceptElement[] concepts, CQTable table, QPNode child) {
		super(child);
		this.concepts = concepts;
		this.table = table;
	}

	@Override
	public void nextBlock(Block block) {
		super.nextBlock(block);
		currentRow = entity.getCBlocks().get(table.getResolvedConnector(), block);
	}

	@Override
	public void nextEvent(Block block, int event) {
		if (active) {
			//check concepts
			int[] mostSpecificChildren;
			if (currentRow.getCBlock().getMostSpecificChildren() != null
				&& ((mostSpecificChildren = currentRow.getCBlock().getMostSpecificChildren().get(event)) != null)) {

				for (ConceptElement ce : concepts) { //see #177  we could improve this by building a a prefix tree over concepts.prefix
					if (ce.matchesPrefix(mostSpecificChildren)) {
						getChild().nextEvent(block, event);
					}
				}
			}
			else {
				for (ConceptElement ce : concepts) { //see #178  we could improve this by building a a prefix tree over concepts.prefix
					if (ce.getConcept() == ce) {
						getChild().nextEvent(block, event);
					}
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
		return new ConceptNode(concepts, table, getChild().clone(ctx));
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		super.collectRequiredTables(requiredTables);
		requiredTables.add(table.getResolvedConnector().getTable().getId());
	}
	
	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		active = table.getResolvedConnector().getTable().equals(currentTable);
		if(active)
			super.nextTable(ctx, currentTable);
	}
}

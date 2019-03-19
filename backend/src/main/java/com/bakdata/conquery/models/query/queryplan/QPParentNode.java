package com.bakdata.conquery.models.query.queryplan;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public abstract class QPParentNode extends QPNode {

	private final List<QPNode> children;
	private final ListMultimap<TableId, QPNode> childMap;
	
	protected List<QPNode> currentTableChildren;
	
	public QPParentNode(List<QPNode> children) {
		if(children == null || children.isEmpty()) {
			throw new IllegalArgumentException("A ParentAggregator needs at least one child.");
		}
		this.children = children;
		this.childMap = children
				.stream()
				.flatMap(
					c -> c
						.collectRequiredTables()
						.stream()
						.map(t -> Pair.of(t, c))
				)
				.collect(ImmutableListMultimap
					.toImmutableListMultimap(Pair::getLeft, Pair::getRight)
				);
	}
	
	@Override
	public void init(Entity entity) {
		super.init(entity);
		for(QPNode c:children) {
			c.init(entity);
		}
	}
	
	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		for(QPNode c:children) {
			c.collectRequiredTables(requiredTables);
		}
	}
	
	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		super.nextTable(ctx, currentTable);
		currentTableChildren = childMap.get(currentTable.getId());
		for(QPNode agg:currentTableChildren) {
			agg.nextTable(ctx, currentTable);
		}
	}
	
	@Override
	public void nextBlock(Block block) {
		for(QPNode agg:currentTableChildren) {
			agg.nextBlock(block);
		}
	}
	
	@Override
	public void nextEvent(Block block, int event) {
		for(QPNode agg:currentTableChildren) {
			agg.nextEvent(block, event);
		}
	}
	
	@Override
	public String toString() {
		return super.toString()+"[children = "+children+"]";
	}
}

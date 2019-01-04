package com.bakdata.conquery.models.query.queryplan;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multiset;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public abstract class QPParentNode extends QPNode {

	private final List<QPNode> children;
	private final ListMultimap<Table, QPNode> childMap;
	
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
						.elementSet()
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
		for(QPNode a:children) {
			a.init(entity);
		}
	}
	
	@Override
	public Multiset<Table> collectRequiredTables() {
		Multiset<Table> tables = children.get(0).collectRequiredTables();
		for(int i=1; i<children.size(); i++) {
			tables.addAll(children.get(1).collectRequiredTables());
		}
		return tables;
	}
	
	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		super.nextTable(ctx, currentTable);
		currentTableChildren = childMap.get(currentTable);
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
	public String toString() {
		return super.toString()+"[children = "+children+"]";
	}
}

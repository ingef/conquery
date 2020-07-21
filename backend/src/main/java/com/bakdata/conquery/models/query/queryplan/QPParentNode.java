package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;

@Getter @Setter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
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
		for(int i=0,size=children.size();i<size;i++) {
			children.get(i).init(entity);
		}
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		for(int i=0,size=children.size();i<size;i++) {
			children.get(i).collectRequiredTables(requiredTables);
		}
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		super.nextTable(ctx, currentTable);
		currentTableChildren = childMap.get(currentTable.getId());
		for(int i=0,size=currentTableChildren.size();i<size;i++) {
			currentTableChildren.get(i).nextTable(ctx, currentTable);
		}
	}

	@Override
	public void nextBlock(Bucket bucket) {
		for(int i=0,size=currentTableChildren.size();i<size;i++) {
			currentTableChildren.get(i).nextBlock(bucket);
		}
	}


	@Override
	public boolean isOfInterest(Bucket bucket) {
		boolean interest = false;
		for(int i=0,size=currentTableChildren.size();i<size;i++) {
			interest |= currentTableChildren.get(i).isOfInterest(bucket);
		}
		return interest;
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		boolean interest = false;
		for(int i=0,size=children.size();i<size;i++) {
			interest |= children.get(i).isOfInterest(entity);
		}
		return interest;
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		for(int i=0,size=currentTableChildren.size();i<size;i++) {
			currentTableChildren.get(i).acceptEvent(bucket, event);
		}
	}

	@Override
	public String toString() {
		return super.toString()+"[children = "+children+"]";
	}

	protected Pair<List<QPNode>, ListMultimap<TableId, QPNode>> createClonedFields(CloneContext ctx) {
		List<QPNode> clones = new ArrayList<>(getChildren());
		clones.replaceAll(qp -> ctx.clone(qp));

		ArrayListMultimap<TableId, QPNode> cloneMap = ArrayListMultimap.create(childMap);

		for(Entry<TableId, Collection<QPNode>> e : cloneMap.asMap().entrySet()) {
			((List<QPNode>)e.getValue()).replaceAll(v->ctx.clone(v));
		}
		return Pair.of(clones, cloneMap);
	}
}

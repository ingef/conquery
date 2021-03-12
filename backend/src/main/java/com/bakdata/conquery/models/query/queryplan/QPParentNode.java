package com.bakdata.conquery.models.query.queryplan;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.Map.Entry;

@Getter @Setter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class QPParentNode extends QPNode {

	private final List<QPNode> children;
	private final ListMultimap<TableId, QPNode> childMap;
	protected final DateAggregator dateAggregator;

	protected List<QPNode> currentTableChildren;

	// Just for debugging
	private DateAggregationAction action;


	public QPParentNode(List<QPNode> children, DateAggregationAction action) {
		Preconditions.checkNotNull(action);
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

		// Save action for debugging
		this.action = action;
		this.dateAggregator = new DateAggregator(action);

		for (QPNode child : children) {
			this.dateAggregator.register(child.getDateAggregators());
		}
	}

	@Override
	public void init(Entity entity, QueryExecutionContext ctx) {
		super.init(entity, ctx);
		for (QPNode child : children) {
			child.init(entity, ctx);
		}
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		for (QPNode child : children) {
			child.collectRequiredTables(requiredTables);
		}
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		super.nextTable(ctx, currentTable);
		currentTableChildren = childMap.get(currentTable);

		for (QPNode currentTableChild : currentTableChildren) {
			currentTableChild.nextTable(ctx, currentTable);
		}
	}

	@Override
	public void nextBlock(Bucket bucket) {
		for (QPNode currentTableChild : currentTableChildren) {
			currentTableChild.nextBlock(bucket);
		}
	}


	@Override
	public boolean isOfInterest(Bucket bucket) {
		for (QPNode child : currentTableChildren) {
			if (child.isOfInterest(bucket)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		for (QPNode child : children) {
			if (child.isOfInterest(entity)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		for (QPNode currentTableChild : currentTableChildren) {
			currentTableChild.acceptEvent(bucket, event);
		}
	}

	@Override
	public String toString() {
		return super.toString()+"[children = "+children+"]";
	}

	protected Pair<List<QPNode>, ListMultimap<TableId, QPNode>> createClonedFields(CloneContext ctx) {
		List<QPNode> clones = new ArrayList<>(getChildren());
		clones.replaceAll(ctx::clone);

		ArrayListMultimap<TableId, QPNode> cloneMap = ArrayListMultimap.create(childMap);

		for(Entry<TableId, Collection<QPNode>> e : cloneMap.asMap().entrySet()) {
			((List<QPNode>)e.getValue()).replaceAll(ctx::clone);
		}
		return Pair.of(clones, cloneMap);
	}

	@Override
	public Collection<Aggregator<CDateSet>> getDateAggregators() {
		if(dateAggregator != null) {
			return Set.of(dateAggregator);
		}
		return Collections.emptySet();
	}
}

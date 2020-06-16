package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// TODO: 16.06.2020 FK: Remove this class and flatten QPNode/EventIterating; this class doesn't do much beyond adding complexity.
@RequiredArgsConstructor @Getter @ToString(of = "aggregator")
public class AggregatorNode<T> extends QPNode  {

	private final Aggregator<T> aggregator;
	@Setter(AccessLevel.PROTECTED)
	private boolean triggered = false;
	
	@Override
	public void nextEvent(Bucket bucket, int event) {
		triggered = true;
		aggregator.aggregateEvent(bucket, event);
	}

	@Override
	public boolean isContained() {
		return triggered;
	}
	
	@Override
	public AggregatorNode<T> doClone(CloneContext ctx) {
		return new AggregatorNode<>(aggregator.clone(ctx));
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		aggregator.collectRequiredTables(requiredTables);
	}
	
	@Override
	public void nextBlock(Bucket bucket) {
		aggregator.nextBlock(bucket);
	}
	
	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		aggregator.nextTable(ctx, currentTable);
	}
	
	@Override
	public boolean isOfInterest(Bucket bucket) {
		return true;
	}
	
	@Override
	public boolean isOfInterest(Entity entity) {
		return true;
	}
}

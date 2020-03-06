package com.bakdata.conquery.models.query.queryplan;

import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@NoArgsConstructor
@Getter @Setter
public abstract class QPSwitchNode<KEY> extends QPChainNode {

	private Map<KEY, QPNode> childPerKey = new HashMap<>();
	private Table currentTable;
	private Bucket currentBucket;
	
	public QPSwitchNode(@NonNull QPNode child) {
		super(child);
	}
	
	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		super.nextTable(ctx, currentTable);
		for(QPNode c:childPerKey.values()) {
			c.nextTable(ctx, currentTable);
		}
		this.currentTable = currentTable;
	}
	
	@Override
	public void nextBlock(Bucket bucket) {
		super.nextBlock(bucket);
		for(QPNode c:childPerKey.values()) {
			c.nextBlock(bucket);
		}
	}

	@Override
	public void nextEvent(Bucket bucket, int event) {
		KEY key = rowKey(bucket, event);
		childPerKey
			.computeIfAbsent(key, this::createChild)
			.nextEvent(bucket, event);
	}
	
	private QPNode createChild(KEY key) {
		QPNode c = getChild().clone(new CloneContext(this.getContext().getStorage()));
		c.init(this.getEntity());
		c.nextTable(this.getContext(), currentTable);
		c.nextBlock(currentBucket);
		return c;
	}

	protected abstract KEY rowKey(Bucket bucket, int event);

	@Override
	public boolean isContained() {
		for(QPNode c:childPerKey.values()) {
			if(c.isContained()) {
				return true;
			}
		}
		return false;
	}
}
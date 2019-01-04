package com.bakdata.conquery.models.query.queryplan;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Multiset;

import lombok.Getter;

public abstract class QPNode {
	
	@JsonIgnore @Getter
	private transient OpenResult lastResult = OpenResult.MAYBE;
	protected QueryContext context;
	protected Entity entity;
	
	public void init(Entity entity) {
		this.entity = entity;
		init();
	}
	
	protected void init() {}

	public abstract QPNode clone(QueryPlan plan, QueryPlan clone);
	
	public abstract Multiset<Table> collectRequiredTables();

	public void nextTable(QueryContext ctx, Table currentTable) {
		this.context = ctx;
	}
	
	public void nextBlock(Block block) {}

	/**
	 * If Node has not yet signaled termination, continue aggregating over events.
	 *
	 * @param block
	 * @param event
	 * @return
	 */
	public final OpenResult aggregate(Block block, int event) {
		if(lastResult == OpenResult.MAYBE) {
			lastResult = nextEvent(block, event);
		}
		return lastResult;
	}

	protected abstract OpenResult nextEvent(Block block, int event);

	public boolean isContained() {
		return getLastResult().asDefiniteResult();
	}
	
	public List<QPNode> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}

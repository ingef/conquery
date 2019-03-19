package com.bakdata.conquery.models.query.queryplan;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.clone.CtxCloneable;

public abstract class QPNode implements EventIterating, CtxCloneable<QPNode> {
	protected QueryContext context;
	protected Entity entity;

	public void init(Entity entity) {
		this.entity = entity;
		init();
	}

	protected void init() {
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		this.context = ctx;
	}

	public abstract void nextEvent(Block block, int event);

	public abstract boolean isContained();

	public List<QPNode> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}

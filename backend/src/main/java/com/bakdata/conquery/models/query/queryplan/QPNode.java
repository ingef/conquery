package com.bakdata.conquery.models.query.queryplan;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.clone.CtxCloneable;

public abstract class QPNode implements EventIterating, CtxCloneable<QPNode> {
	private transient boolean lastResult = true;
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

	public final boolean aggregate(Block block, int event) {
		if (lastResult) {
			lastResult = nextEvent(block, event);
		}
		return lastResult;
	}

	public abstract boolean nextEvent(Block block, int event);

	public boolean isContained() {
		return lastResult;
	}

	public List<QPNode> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}

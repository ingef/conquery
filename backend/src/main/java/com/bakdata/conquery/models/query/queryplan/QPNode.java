package com.bakdata.conquery.models.query.queryplan;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.clone.CtxCloneable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter(AccessLevel.PROTECTED) @Setter(AccessLevel.PROTECTED)
public abstract class QPNode implements EventIterating, CtxCloneable<QPNode> {
	protected QueryExecutionContext context;
	protected Entity entity;

	/**
	 * Initialize the QueryPlan element for evaluation. eg.: Prefetching elements.
	 * @apiNote inheritors should always call super.
	 */
	public void init(Entity entity, QueryExecutionContext context) {
		setEntity(entity);
		setContext(context);
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		setContext(ctx);
	}

	@Override
	public abstract void acceptEvent(Bucket bucket, int event);

	public abstract boolean isContained();

	public List<QPNode> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}

package com.bakdata.conquery.models.query.queryplan.filter;

import java.util.Set;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.EventIterating;
import com.bakdata.conquery.models.query.queryplan.clone.CtxCloneable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public abstract class FilterNode<FILTER_VALUE> implements EventIterating, CtxCloneable<FilterNode<FILTER_VALUE>> {

	@Setter @Getter
	protected FILTER_VALUE filterValue;

	@Override
	public abstract void collectRequiredTables(Set<TableId> requiredTables);

	public boolean checkEvent(Bucket bucket, int event) {
		return true;
	}

	public abstract void acceptEvent(Bucket bucket, int event);

	public abstract boolean isContained();
	
	@Override
	public boolean isOfInterest(Bucket bucket) {
		return true;
	}
	
	@Override
	public boolean isOfInterest(Entity entity) {
		return true;
	}
	
}

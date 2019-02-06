package com.bakdata.conquery.models.query.queryplan.aggregators;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.EventIterating;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString(of = "id")
@AllArgsConstructor
public abstract class Aggregator<T> implements Cloneable, EventIterating {

	@Getter
	private SelectId id;

	public abstract T getAggregationResult();

	public abstract void aggregateEvent(Block block, int event);

	public abstract Aggregator<T> clone();
}

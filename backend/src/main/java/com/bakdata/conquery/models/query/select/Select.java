package com.bakdata.conquery.models.query.select;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.Named;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.specific.AggregatorNode;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public abstract class Select extends Labeled<SelectId> {

	@JsonBackReference
	private Connector connector;

	public AggregatorNode<?> createAggregator(int position) {
		return new AggregatorNode<>(position, createAggregator());
	}

	protected abstract Aggregator<?> createAggregator();

	@Override
	public SelectId createId() {
		return new SelectId(connector.getId(), getName());
	}
}

package com.bakdata.conquery.models.concepts.select;

import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorSelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.specific.AggregatorNode;
import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Getter;

public abstract class ConnectorSelect extends Labeled<ConnectorSelectId> implements Select<ConnectorSelectId> {

	@JsonBackReference
	private Connector connector;

	@Getter
	private String description;

	@Override
	public AggregatorNode<?> createAggregator(int position) {
		return new AggregatorNode<>(position, createAggregator());
	}

	protected abstract Aggregator<?> createAggregator();

	@Override
	public ConnectorSelectId createId() {
		return new ConnectorSelectId(connector.getId(), getName());
	}
}

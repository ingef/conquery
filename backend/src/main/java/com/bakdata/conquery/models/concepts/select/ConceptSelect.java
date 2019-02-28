package com.bakdata.conquery.models.concepts.select;

import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptSelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.specific.AggregatorNode;
import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Getter;

public abstract class ConceptSelect extends Labeled<ConceptSelectId> implements Select<ConceptSelectId> {

	@Getter
	@JsonBackReference
	private Concept<?> concept;

	@Getter
	private String description;

	@Override
	public AggregatorNode<?> createAggregator(int position) {
		return new AggregatorNode<>(position, createAggregator());
	}

	protected abstract Aggregator<?> createAggregator();

	@Override
	public ConceptSelectId createId() {
		return new ConceptSelectId(concept.getId(), getName());
	}
}

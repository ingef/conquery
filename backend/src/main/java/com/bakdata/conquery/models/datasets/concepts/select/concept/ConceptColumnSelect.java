package com.bakdata.conquery.models.datasets.concepts.select.concept;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.ConceptElementsAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.ConceptValuesAggregator;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Setter;

@CPSType(id = "CONCEPT_VALUES", base = Select.class)
public class ConceptColumnSelect extends Select {

	private boolean resolved = false;

	@JsonBackReference
	@JsonIgnore
	@Setter
	private TreeConcept concept;

	@Override
	public Aggregator<?> createAggregator() {
		if(resolved){
			return new ConceptElementsAggregator(concept);
		}

		return new ConceptValuesAggregator(concept);
	}



}

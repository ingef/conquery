package com.bakdata.conquery.models.datasets.concepts.select.concept;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.ConceptElementsAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.ConceptValuesAggregator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Data;

/**
 * Select to extract the values used to build a {@link TreeConcept}.
 */
@CPSType(id = "CONCEPT_VALUES", base = Select.class)
@Data
public class ConceptColumnSelect extends UniversalSelect {

	/**
	 * If true, values are returned as resolved {@link ConceptElement#getLabel()} instead of the actual values.
	 */
	private boolean asIds = false;

	@Override
	public Aggregator<?> createAggregator() {
		if(isAsIds()){
			return new ConceptElementsAggregator(((TreeConcept) getHolder().findConcept()));
		}

		return new ConceptValuesAggregator(((TreeConcept) getHolder().findConcept()));
	}

	@JsonIgnore
	@ValidationMethod(message = "Holder must be TreeConcept.")
	public boolean isHolderTreeConcept() {
		return getHolder().findConcept() instanceof TreeConcept;
	}



}

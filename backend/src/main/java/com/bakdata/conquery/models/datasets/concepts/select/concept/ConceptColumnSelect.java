package com.bakdata.conquery.models.datasets.concepts.select.concept;

import java.util.Set;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.ConceptElementsAggregator;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.types.SemanticType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Setter;

@CPSType(id = "CONCEPT_VALUES", base = Select.class)
public class ConceptColumnSelect extends Select {

	@JsonBackReference
	@JsonIgnore
	@Setter
	private TreeConcept concept;

	@Override
	public Aggregator<?> createAggregator() {
		return new ConceptElementsAggregator(concept);
	}

	@Override
	public SelectResultInfo getResultInfo(CQConcept cqConcept) {
		return new SelectResultInfo(this, cqConcept, Set.of(new SemanticType.ConceptColumnT(concept)));
	}

}
